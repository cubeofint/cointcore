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
        CurrencyMovement draft = new CurrencyMovement(
                0L,
                System.currentTimeMillis(),
                fromId,
                fromName,
                toId,
                toName,
                Math.max(0L, amount),
                type,
                note
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
        CurrencyMovementSink azLink = AzLinkCurrencyMovementSink.tryCreate();
        CurrencyMovementSink http = settings.enabled() && !settings.endpointUrl().isBlank()
                ? new HttpCurrencyMovementSink(settings)
                : CurrencyMovementSink.NOOP;
        return movement -> {
            azLink.enqueue(movement);
            http.enqueue(movement);
        };
    }

    /**
     * Soft AzLink adapter. No compile dependency. Never sets an absolute site balance.
     * Looks for an append/queue API; if none is found, stays a no-op.
     */
    static final class AzLinkCurrencyMovementSink implements CurrencyMovementSink {
        private AzLinkCurrencyMovementSink() {
        }

        static CurrencyMovementSink tryCreate() {
            String[] candidates = {
                    "com.azlink.api.AzLink",
                    "net.azlink.AzLink",
                    "com.azuriom.azlink.common.AzLinkApi"
            };
            for (String className : candidates) {
                try {
                    Class.forName(className, false, CurrencyMovementService.class.getClassLoader());
                    LOGGER.info(
                            "AzLink class {} is present, but cointcore has no known append-only movement API. "
                                    + "Leaving the durable outbox local. See README (currency movements / AzLink).",
                            className
                    );
                } catch (ClassNotFoundException ignored) {
                }
            }
            return CurrencyMovementSink.NOOP;
        }

        @Override
        public void enqueue(CurrencyMovement movement) {
        }
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
