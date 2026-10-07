package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.CurrencyMovementConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public final class CurrencyMovementService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile CurrencyMovementSink sink = CurrencyMovementSink.NOOP;

    private CurrencyMovementService() {
    }

    public static void refreshSink() {
        sink = createSink(CurrencyMovementConfig.get());
    }

    public static CurrencyMovement record(
            MinecraftServer server,
            UUID fromId,
            String fromName,
            UUID toId,
            String toName,
            long amount,
            CurrencyMovementType type,
            String note
    ) {
        return record(server, fromId, fromName, toId, toName, amount, type, note, List.of(), null);
    }

    public static CurrencyMovement record(
            MinecraftServer server,
            UUID fromId,
            String fromName,
            UUID toId,
            String toName,
            long amount,
            CurrencyMovementType type,
            String note,
            List<CurrencyMovement.Delta> deltas,
            String siteOpId
    ) {
        CurrencyMovement draft = new CurrencyMovement(
                0L,
                System.currentTimeMillis(),
                fromId,
                fromName,
                toId,
                toName,
                Math.max(0L, amount),
                type,
                note,
                deltas,
                siteOpId
        );
        CurrencyMovement stored = CurrencyMovementSavedData.get(server).append(draft);
        try {
            sink.enqueue(stored);
        } catch (RuntimeException exception) {
            LOGGER.warn("Currency movement sink failed for id {}", stored.id(), exception);
        }
        return stored;
    }

    private static CurrencyMovementSink createSink(CurrencyMovementConfig.Settings settings) {
        // Site delivery of the outbox is SiteMovementSender (AzLink postMovements, config-gated).
        // This optional HTTP sink is a leftover debug hook and must not set an absolute site balance.
        if (settings.enabled() && !settings.endpointUrl().isBlank()) {
            return new HttpCurrencyMovementSink(settings);
        }
        return CurrencyMovementSink.NOOP;
    }

    static final class HttpCurrencyMovementSink implements CurrencyMovementSink {
        private final CurrencyMovementConfig.Settings settings;
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5L))
                .build();

        HttpCurrencyMovementSink(CurrencyMovementConfig.Settings settings) {
            this.settings = settings;
        }

        @Override
        public void enqueue(CurrencyMovement movement) {
            try {
                String body = "{\"id\":" + movement.id()
                        + ",\"timestamp\":" + movement.timestampMs()
                        + ",\"type\":\"" + jsonEscape(movement.type().id()) + "\""
                        + ",\"amount\":" + movement.amount()
                        + ",\"from_id\":" + uuidJson(movement.fromId())
                        + ",\"from_name\":" + stringJson(movement.fromName())
                        + ",\"to_id\":" + uuidJson(movement.toId())
                        + ",\"to_name\":" + stringJson(movement.toName())
                        + ",\"note\":" + stringJson(movement.note())
                        + "}";
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(settings.endpointUrl()))
                        .timeout(Duration.ofSeconds(10L))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                if (!settings.authorizationHeader().isBlank()) {
                    builder.header("Authorization", settings.authorizationHeader());
                }
                client.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding())
                        .exceptionally(error -> {
                            LOGGER.warn("Currency movement HTTP post failed for id {}", movement.id(), error);
                            return null;
                        });
            } catch (RuntimeException exception) {
                LOGGER.warn("Currency movement HTTP enqueue failed for id {}", movement.id(), exception);
            }
        }

        private static String uuidJson(UUID id) {
            return id == null ? "null" : "\"" + id + "\"";
        }

        private static String stringJson(String value) {
            return value == null ? "null" : "\"" + jsonEscape(value) + "\"";
        }

        private static String jsonEscape(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
