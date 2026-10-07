package com.mawlee.cointcore.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mawlee.cointcore.config.CurrencyMovementConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends the durable currency-movement outbox to the site in batches through AzLink.
 * The site stores each (server, movement id) once, so a resend after a lost response is harmless.
 * On failure waits with exponential backoff (5s doubling up to 5 min). Off by default.
 */
public final class SiteMovementSender {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BRIDGE = "com.azuriom.azlink.common.coins.CoinOperationsBridge";
    private static final long MIN_BACKOFF_MS = SiteMovementPayload.MIN_BACKOFF_MS;
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean();
    private static volatile long nextAttemptAt;
    private static volatile long backoffMs;
    private static Method isAvailable;
    private static Method postMovements;
    private static boolean resolved;

    private SiteMovementSender() {
    }

    public static void tick(MinecraftServer server) {
        CurrencyMovementConfig.Settings settings = CurrencyMovementConfig.get();
        long now = System.currentTimeMillis();
        if (!settings.siteMovementsEnabled() || now < nextAttemptAt || !resolve()
                || !IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!Boolean.TRUE.equals(isAvailable.invoke(null))) {
                fail(now);
                return;
            }
            CurrencyMovementSavedData data = CurrencyMovementSavedData.get(server);
            List<CurrencyMovement> batch = data.unsent(settings.siteMovementsBatch());
            if (batch.isEmpty()) {
                nextAttemptAt = now + MIN_BACKOFF_MS;
                IN_FLIGHT.set(false);
                return;
            }
            long lastId = batch.get(batch.size() - 1).id();
            @SuppressWarnings("unchecked")
            CompletableFuture<String> future = (CompletableFuture<String>) postMovements.invoke(null, toJson(batch));
            future.whenComplete((json, error) -> {
                try {
                    if (error != null) {
                        LOGGER.debug("Currency movements upload failed: {}", error.toString());
                        fail(System.currentTimeMillis());
                        return;
                    }
                    long accepted = SiteMovementPayload.acceptedUpTo(json, lastId);
                    server.execute(() -> data.markSiteSentUpTo(accepted));
                    backoffMs = 0L;
                    // More pending? send the next batch on the next tick.
                    nextAttemptAt = batch.size() >= settings.siteMovementsBatch() ? 0L : System.currentTimeMillis() + MIN_BACKOFF_MS;
                } finally {
                    IN_FLIGHT.set(false);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Currency movements upload failed", e);
            fail(now);
        }
    }

    private static void fail(long now) {
        backoffMs = SiteMovementPayload.nextBackoff(backoffMs);
        nextAttemptAt = now + backoffMs;
        IN_FLIGHT.set(false);
    }

    static String toJson(List<CurrencyMovement> batch) {
        JsonArray array = new JsonArray();
        for (CurrencyMovement m : batch) {
            JsonObject o = new JsonObject();
            o.addProperty("id", m.id());
            o.addProperty("timestamp", m.timestampMs());
            o.addProperty("type", m.type().id());
            o.addProperty("amount", m.amount());
            o.addProperty("from_id", uuid(m.fromId()));
            o.addProperty("from_name", m.fromName());
            o.addProperty("to_id", uuid(m.toId()));
            o.addProperty("to_name", m.toName());
            o.addProperty("note", m.note());
            array.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("movements", array);
        return root.toString();
    }

    private static String uuid(UUID id) {
        return id == null ? null : id.toString();
    }

    private static synchronized boolean resolve() {
        if (resolved) {
            return postMovements != null;
        }
        resolved = true;
        try {
            Class<?> c = Class.forName(BRIDGE, true, SiteMovementSender.class.getClassLoader());
            isAvailable = c.getMethod("isAvailable");
            postMovements = c.getMethod("postMovements", String.class);
            LOGGER.info("AzLink movement bridge found; currency movements will be sent to the site");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Currency movement upload enabled but AzLink {}.postMovements not found", BRIDGE);
            return false;
        }
    }
}
