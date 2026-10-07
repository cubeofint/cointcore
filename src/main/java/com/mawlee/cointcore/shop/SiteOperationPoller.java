package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.CurrencyMovementConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pulls queued site&lt;-&gt;server gluon transfers through AzLink and applies them to the server wallet.
 * Works for offline players: the wallet is keyed by UUID, no login is required.
 * Each operation id is applied at most once (local {@link SiteOperationSavedData} + site-side ack idempotency).
 */
public final class SiteOperationPoller {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BRIDGE = "com.azuriom.azlink.common.coins.CoinOperationsBridge";
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean();
    private static int ticks;
    private static Method isAvailable;
    private static Method fetchPending;
    private static Method ack;
    private static boolean bridgeResolved;

    private SiteOperationPoller() {
    }

    /** Call once per server tick (end phase). */
    public static void tick(MinecraftServer server) {
        CurrencyMovementConfig.Settings settings = CurrencyMovementConfig.get();
        if (!settings.siteQueueEnabled()) {
            return;
        }
        if (++ticks < settings.siteQueuePollSeconds() * 20) {
            return;
        }
        ticks = 0;
        if (!resolveBridge() || !IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!Boolean.TRUE.equals(isAvailable.invoke(null))) {
                IN_FLIGHT.set(false);
                return;
            }
            @SuppressWarnings("unchecked")
            CompletableFuture<String> future = (CompletableFuture<String>) fetchPending.invoke(null, 50);
            future.whenComplete((json, error) -> {
                if (error != null) {
                    LOGGER.debug("Site operation fetch failed: {}", error.toString());
                    IN_FLIGHT.set(false);
                    return;
                }
                List<SiteOperation> ops = parse(json);
                if (ops.isEmpty()) {
                    IN_FLIGHT.set(false);
                    return;
                }
                server.execute(() -> {
                    try {
                        for (SiteOperation op : ops) {
                            String status = apply(server, op);
                            sendAck(op.id(), status, "failed".equals(status) ? "insufficient_server_balance" : null);
                        }
                    } finally {
                        IN_FLIGHT.set(false);
                    }
                });
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            IN_FLIGHT.set(false);
            LOGGER.warn("Site operation poll failed", e);
        }
    }

    /** Applies one operation on the server thread. Returns "applied" or "failed". Package-private for tests. */
    static String apply(MinecraftServer server, SiteOperation op) {
        SiteOperationSavedData handled = SiteOperationSavedData.get(server);
        String previous = handled.status(op.id());
        if (previous != null) {
            return previous;
        }
        String status;
        if (op.toServer()) {
            GluonWallet.add(server, op.playerId(), op.amount());
            status = "applied";
        } else {
            status = GluonWallet.trySubtract(server, op.playerId(), op.amount()) ? "applied" : "failed";
        }
        handled.mark(op.id(), status);
        if ("applied".equals(status)) {
            CurrencyMovementService.record(server,
                    op.toServer() ? null : op.playerId(), op.toServer() ? "site" : op.playerName(),
                    op.toServer() ? op.playerId() : null, op.toServer() ? op.playerName() : "site",
                    op.amount(),
                    op.toServer() ? CurrencyMovementType.SITE_TO_SERVER : CurrencyMovementType.SERVER_TO_SITE,
                    "site-op:" + op.id());
        }
        LOGGER.info("Site gluon operation {} {} {} for {} -> {}", op.id(), op.toServer() ? "+" : "-",
                op.amount(), op.playerId(), status);
        return status;
    }

    static List<SiteOperation> parse(String json) {
        try {
            return SiteOperation.parseAll(json, malformedId -> {
                LOGGER.warn("Skipping malformed site operation {}", malformedId);
                sendAck(malformedId, "failed", "malformed_operation");
            });
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to parse site operations response", e);
            return List.of();
        }
    }

    private static void sendAck(String id, String status, String error) {
        try {
            @SuppressWarnings("unchecked")
            CompletableFuture<String> f = (CompletableFuture<String>) ack.invoke(null, id, status, error);
            f.exceptionally(e -> {
                // Op stays pending on the site and is re-delivered; local dedup keeps it exactly-once.
                LOGGER.debug("Ack for site operation {} failed: {}", id, e.toString());
                return null;
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Ack for site operation {} failed", id, e);
        }
    }

    private static synchronized boolean resolveBridge() {
        if (bridgeResolved) {
            return fetchPending != null;
        }
        bridgeResolved = true;
        try {
            Class<?> c = Class.forName(BRIDGE, true, SiteOperationPoller.class.getClassLoader());
            isAvailable = c.getMethod("isAvailable");
            fetchPending = c.getMethod("fetchPending", int.class);
            ack = c.getMethod("ack", String.class, String.class, String.class);
            LOGGER.info("AzLink coin operation bridge found; site gluon queue polling active");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Site gluon queue enabled but AzLink {} not found; polling disabled", BRIDGE);
            return false;
        }
    }
}
