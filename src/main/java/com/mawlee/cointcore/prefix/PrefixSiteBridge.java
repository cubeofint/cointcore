package com.mawlee.cointcore.prefix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mawlee.cointcore.player.PlayerIdentityResolve;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two-way prefix sync with Azuriom through AzLink.
 *
 * <p>Game → site: {@code POST /api/azlink/prefixes/report}.
 * Site → game: {@code GET /api/azlink/prefixes/operations} then
 * {@code POST /api/azlink/prefixes/ack}.
 */
public final class PrefixSiteBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean REPORT_IN_FLIGHT = new AtomicBoolean();
    private static final AtomicBoolean POLL_IN_FLIGHT = new AtomicBoolean();
    private static int pollTicks;

    private PrefixSiteBridge() {
    }

    public static void report(MinecraftServer server, List<UUID> playerIds) {
        if (!PrefixConfig.get().siteSyncEnabled() || playerIds == null || playerIds.isEmpty()) {
            return;
        }
        if (!LuckPermsIntegration.isAvailable() || !AzLinkHttp.isAvailable()) {
            return;
        }
        if (!REPORT_IN_FLIGHT.compareAndSet(false, true)) {
            for (UUID playerId : playerIds) {
                PrefixReportScheduler.request(server, playerId);
            }
            return;
        }
        List<UUID> copy = List.copyOf(playerIds);
        loadReports(server, copy, 0, new ArrayList<>()).whenComplete((players, error) -> {
            if (error != null) {
                LOGGER.debug("Prefix report build failed: {}", error.toString());
                REPORT_IN_FLIGHT.set(false);
                return;
            }
            JsonObject body = new JsonObject();
            JsonArray array = new JsonArray();
            for (JsonObject player : players) {
                array.add(player);
            }
            body.add("players", array);
            AzLinkHttp.reportPrefixes(body.toString()).whenComplete((ignored, sendError) -> {
                if (sendError != null) {
                    LOGGER.debug("Prefix report POST failed: {}", sendError.toString());
                }
                REPORT_IN_FLIGHT.set(false);
            });
        });
    }

    public static void tick(MinecraftServer server) {
        PrefixConfig.Settings settings = PrefixConfig.get();
        if (!settings.enabled() || !settings.siteSyncEnabled()) {
            return;
        }
        if (++pollTicks < settings.sitePollSeconds() * 20) {
            return;
        }
        pollTicks = 0;
        poll(server);
    }

    static void poll(MinecraftServer server) {
        if (!LuckPermsIntegration.isAvailable() || !AzLinkHttp.isAvailable()
                || !POLL_IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }
        AzLinkHttp.fetchPrefixOperations(50).whenComplete((json, error) -> {
            if (error != null) {
                LOGGER.debug("Prefix operations fetch failed: {}", error.toString());
                POLL_IN_FLIGHT.set(false);
                return;
            }
            List<PrefixSiteOperation> ops = PrefixSiteOperation.parseAll(json, malformedId -> {
                LOGGER.warn("Skipping malformed prefix operation {}", malformedId);
                AzLinkHttp.ackPrefix(malformedId, "rejected", "malformed_operation", null);
            });
            if (ops.isEmpty()) {
                POLL_IN_FLIGHT.set(false);
                return;
            }
            server.execute(() -> {
                try {
                    applyAll(server, ops);
                } finally {
                    POLL_IN_FLIGHT.set(false);
                }
            });
        });
    }

    private static void applyAll(MinecraftServer server, List<PrefixSiteOperation> ops) {
        PrefixOperationSavedData handled = PrefixOperationSavedData.get(server);
        for (PrefixSiteOperation op : ops) {
            PrefixOperationSavedData.Outcome previous = handled.outcome(op.opId());
            if (previous != null) {
                AzLinkHttp.ackPrefix(op.opId(), previous.status(), previous.reason(), previous.selected());
                continue;
            }
            applyOne(server, handled, op);
        }
    }

    private static void applyOne(
            MinecraftServer server,
            PrefixOperationSavedData handled,
            PrefixSiteOperation op
    ) {
        PrefixSelectionService.select(server, op.playerId(), op.group(), decision -> {
            String status = switch (decision.kind()) {
                case APPLIED, AUTO, UNCHANGED -> "applied";
                case UNAVAILABLE -> "rejected";
            };
            String selected = switch (decision.kind()) {
                case APPLIED, UNCHANGED -> decision.selectedGroup();
                case AUTO, UNAVAILABLE -> null;
            };
            handled.mark(op.opId(), status, decision.reason(), selected);
            AzLinkHttp.ackPrefix(op.opId(), status, decision.reason(), selected);
            LOGGER.info("Site prefix_select {} for {} group={} -> {} {}",
                    op.opId(), op.playerId(), op.group(), status, decision.reason());
        });
    }

    private static java.util.concurrent.CompletableFuture<List<JsonObject>> loadReports(
            MinecraftServer server,
            List<UUID> playerIds,
            int index,
            List<JsonObject> acc
    ) {
        if (index >= playerIds.size()) {
            return java.util.concurrent.CompletableFuture.completedFuture(acc);
        }
        UUID playerId = playerIds.get(index);
        return LuckPermsUsers.loadSnapshot(playerId).handle((snapshot, error) -> {
            if (error == null && snapshot != null) {
                acc.add(toReport(server, playerId, snapshot));
            }
            return acc;
        }).thenCompose(next -> loadReports(server, playerIds, index + 1, next));
    }

    private static JsonObject toReport(MinecraftServer server, UUID playerId, PrefixSnapshot snapshot) {
        JsonObject player = new JsonObject();
        player.addProperty("uuid", playerId.toString());
        player.addProperty(
                "name",
                PlayerIdentityResolve.resolveName(server, playerId)
                        .or(() -> LuckPermsUsers.loadedUsername(playerId))
                        .orElse(playerId.toString())
        );
        JsonArray available = new JsonArray();
        for (PrefixOption option : snapshot.available()) {
            JsonObject row = new JsonObject();
            row.addProperty("group", option.group());
            row.addProperty("prefix_raw", option.prefixRaw());
            row.addProperty("category", option.category());
            available.add(row);
        }
        player.add("available", available);
        if (snapshot.selected() == null) {
            player.add("selected", null);
        } else {
            player.addProperty("selected", snapshot.selected());
        }
        player.addProperty("locked_staff", false);
        return player;
    }

    static JsonObject playerReport(MinecraftServer server, UUID playerId, PrefixSnapshot snapshot) {
        return toReport(server, playerId, snapshot);
    }

    public static CompletableFuture<Void> postReports(List<JsonObject> players) {
        if (players == null || players.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        for (JsonObject player : players) {
            array.add(player);
        }
        body.add("players", array);
        return AzLinkHttp.reportPrefixes(body.toString()).thenApply(ignored -> null);
    }
}
