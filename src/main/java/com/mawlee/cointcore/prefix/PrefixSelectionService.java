package com.mawlee.cointcore.prefix;

import com.mawlee.cointcore.config.PrefixConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Applies {@link PrefixSelectionRules} to LuckPerms users and notifies the site.
 */
public final class PrefixSelectionService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    private PrefixSelectionService() {
    }

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        if (!LuckPermsIntegration.isAvailable()) {
            LOGGER.info("Prefix selection inactive: LuckPerms is not loaded");
            return;
        }
        LuckPermsIntegration.registerUserDataRecalculateListener(playerId -> {
            MinecraftServer current = server;
            if (current == null) {
                return;
            }
            current.execute(() -> reconcile(current, playerId, true));
        });
        LOGGER.info("Prefix selection ready (ranks track={}, priority={})",
                PrefixConfig.get().ranksTrack(), PrefixConfig.get().prefixPriority());
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!PrefixConfig.get().enabled()) {
            return;
        }
        reconcile(player.server, player.getUUID(), true);
    }

    public static void reconcile(MinecraftServer minecraftServer, UUID playerId, boolean report) {
        if (!PrefixConfig.get().enabled() || !LuckPermsIntegration.isAvailable() || playerId == null) {
            return;
        }
        LuckPermsUsers.loadSnapshot(playerId).whenComplete((snapshot, error) -> {
            if (error != null) {
                LOGGER.debug("Prefix reconcile load failed for {}: {}", playerId, error.toString());
                return;
            }
            minecraftServer.execute(() -> applyReconcile(minecraftServer, playerId, snapshot, report));
        });
    }

    private static void applyReconcile(
            MinecraftServer minecraftServer,
            UUID playerId,
            PrefixSnapshot snapshot,
            boolean report
    ) {
        PrefixSelectionRules.Decision decision = PrefixSelectionRules.reconcile(
                snapshot.selected(),
                snapshot.availableGroups()
        );
        if (decision.clearOverride()) {
            applyAutoRender(playerId, snapshot).whenComplete((ignored, error) -> {
                if (error != null) {
                    LOGGER.warn("Failed to drop quest prefix override for {}", playerId, error);
                    return;
                }
                if (report) {
                    PrefixReportScheduler.request(minecraftServer, playerId);
                }
            });
            return;
        }
        if (snapshot.selected() == null) {
            applyAutoRender(playerId, snapshot).whenComplete((ignored, error) -> {
                if (error != null) {
                    LOGGER.debug("Failed to refresh auto quest prefix for {}", playerId, error);
                }
                if (report) {
                    PrefixReportScheduler.request(minecraftServer, playerId);
                }
            });
            return;
        }
        if (report) {
            PrefixReportScheduler.request(minecraftServer, playerId);
        }
    }

    public static void select(
            MinecraftServer minecraftServer,
            UUID playerId,
            String requestedGroup,
            Consumer<PrefixSelectionRules.Decision> callback
    ) {
        if (!PrefixConfig.get().enabled()) {
            callback.accept(PrefixSelectionRules.Decision.unavailable(requestedGroup));
            return;
        }
        if (!LuckPermsIntegration.isAvailable()) {
            callback.accept(PrefixSelectionRules.Decision.unavailable(requestedGroup));
            return;
        }
        LuckPermsUsers.loadSnapshot(playerId).whenComplete((snapshot, error) -> {
            if (error != null) {
                LOGGER.warn("Prefix select load failed for {}", playerId, error);
                minecraftServer.execute(() -> callback.accept(PrefixSelectionRules.Decision.failed()));
                return;
            }
            minecraftServer.execute(() -> applySelect(minecraftServer, playerId, snapshot, requestedGroup, callback));
        });
    }

    public static void selectAuto(
            MinecraftServer minecraftServer,
            UUID playerId,
            Consumer<PrefixSelectionRules.Decision> callback
    ) {
        select(minecraftServer, playerId, null, callback);
    }

    private static void applySelect(
            MinecraftServer minecraftServer,
            UUID playerId,
            PrefixSnapshot snapshot,
            String requestedGroup,
            Consumer<PrefixSelectionRules.Decision> callback
    ) {
        PrefixSelectionRules.Decision decision = requestedGroup == null
                ? PrefixSelectionRules.decideAuto()
                : PrefixSelectionRules.decideSet(snapshot.availableGroups(), requestedGroup);
        CompletableFuture<Void> mutation = switch (decision.kind()) {
            case AUTO -> applyAutoRender(playerId, snapshot);
            case APPLIED -> {
                PrefixOption option = snapshot.option(decision.selectedGroup());
                if (option == null) {
                    yield CompletableFuture.completedFuture(null);
                }
                yield LuckPermsUsers.syncQuestPrefix(playerId, option.group(), option.prefixRaw());
            }
            case UNAVAILABLE, UNCHANGED, FAILED -> CompletableFuture.completedFuture(null);
        };
        mutation.whenComplete((ignored, error) -> minecraftServer.execute(() -> {
            if (error != null) {
                LOGGER.warn("Prefix mutation failed for {}", playerId, error);
                callback.accept(PrefixSelectionRules.Decision.failed());
                return;
            }
            if (decision.kind() != PrefixSelectionRules.Kind.UNAVAILABLE
                    && decision.kind() != PrefixSelectionRules.Kind.FAILED) {
                PrefixReportScheduler.request(minecraftServer, playerId);
            }
            callback.accept(decision);
        }));
    }

    private static CompletableFuture<Void> applyAutoRender(UUID playerId, PrefixSnapshot snapshot) {
        PrefixOption highest = snapshot.highestQuest();
        if (highest == null) {
            return LuckPermsUsers.syncQuestPrefix(playerId, null, null);
        }
        return LuckPermsUsers.syncQuestPrefix(playerId, null, highest.prefixRaw());
    }
}
