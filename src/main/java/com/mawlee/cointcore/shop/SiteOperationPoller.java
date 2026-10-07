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
 * Pulls queued site operations through AzLink and applies them to the server wallet.
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
    private static Method ackWithBalance;
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
                            SiteOperationApply.Decision decision = apply(server, op);
                            sendAck(op.id(), decision.status(), decision.error(), decision.balanceAfter());
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

    /** Applies one operation on the server thread. Package-private for tests. */
    static SiteOperationApply.Decision apply(MinecraftServer server, SiteOperation op) {
        SiteOperationSavedData handled = SiteOperationSavedData.get(server);
        SiteOperationSavedData.Outcome previous = handled.outcome(op.id());
        long current = GluonWallet.get(server, op.playerId());
        if (previous != null) {
            return SiteOperationApply.replay(previous.status(), previous.balanceAfter(), current);
        }
        SiteOperationApply.Decision decision = SiteOperationApply.decide(op.kind(), op.amount(), current);
        if (decision.mutated()) {
            if (decision.signedDelta() > 0L) {
                GluonWallet.add(server, op.playerId(), decision.signedDelta());
            } else if (decision.signedDelta() < 0L) {
                GluonWallet.trySubtract(server, op.playerId(), -decision.signedDelta());
            }
        }
        long balanceAfter = GluonWallet.get(server, op.playerId());
        SiteOperationApply.Decision stored = new SiteOperationApply.Decision(
                decision.status(),
                decision.error(),
                balanceAfter,
                decision.mutated(),
                decision.movementAmount(),
                decision.signedDelta()
        );
        handled.mark(op.id(), stored.status(), stored.balanceAfter());
        if (SiteOperationApply.APPLIED.equals(stored.status())) {
            recordMovement(server, op, stored);
        }
        LOGGER.info("Site gluon operation {} {} {} ({}/{}) for {} -> {} balance_after={}",
                op.id(), describeSign(op), op.amount(), op.kind(), op.source(), op.playerId(), stored.status(),
                stored.balanceAfter());
        return stored;
    }

    private static void recordMovement(MinecraftServer server, SiteOperation op, SiteOperationApply.Decision decision) {
        CurrencyMovementType type = switch (op.kind()) {
            case TO_SERVER -> CurrencyMovementType.SITE_TO_SERVER;
            case FROM_SERVER -> CurrencyMovementType.SERVER_TO_SITE;
            case ADJUST -> CurrencyMovementType.SITE_ADJUST;
        };
        boolean creditPlayer = decision.signedDelta() >= 0L && op.kind() != SiteOperation.Kind.FROM_SERVER;
        List<CurrencyMovement.Delta> deltas = List.of(new CurrencyMovement.Delta(
                op.playerId(), decision.signedDelta(), decision.balanceAfter()));
        CurrencyMovementService.record(
                server,
                creditPlayer ? null : op.playerId(),
                creditPlayer ? "site" : op.playerName(),
                creditPlayer ? op.playerId() : null,
                creditPlayer ? op.playerName() : "site",
                decision.movementAmount(),
                type,
                "site-op:" + op.id(),
                deltas,
                op.id()
        );
    }

    private static String describeSign(SiteOperation op) {
        return switch (op.kind()) {
            case TO_SERVER -> "+";
            case FROM_SERVER -> "-";
            case ADJUST -> op.amount() >= 0L ? "+" : "";
        };
    }

    static List<SiteOperation> parse(String json) {
        try {
            return SiteOperation.parseAll(json, malformedId -> {
                LOGGER.warn("Skipping malformed site operation {}", malformedId);
                sendAck(malformedId, "failed", "malformed_operation", null);
            });
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to parse site operations response", e);
            return List.of();
        }
    }

    private static void sendAck(String id, String status, String error, Long balanceAfter) {
        try {
            boolean useBalance = ackWithBalance != null
                    && (balanceAfter != null || ackWithBalance.getParameterTypes()[3] != long.class);
            @SuppressWarnings("unchecked")
            CompletableFuture<String> f = useBalance
                    ? (CompletableFuture<String>) ackWithBalance.invoke(null, id, status, error, balanceAfter)
                    : (CompletableFuture<String>) ack.invoke(null, id, status, error);
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
            ackWithBalance = resolveAckWithBalance(c);
            LOGGER.info("AzLink coin operation bridge found; site gluon queue polling active");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Site gluon queue enabled but AzLink {} not found; polling disabled", BRIDGE);
            return false;
        }
    }

    private static Method resolveAckWithBalance(Class<?> bridge) {
        try {
            return bridge.getMethod("ackWithBalance", String.class, String.class, String.class, Long.class);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            return bridge.getMethod("ackWithBalance", String.class, String.class, String.class, long.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
