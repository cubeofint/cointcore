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
        LOGGER.info("Prefix selection ready (staff track={}, priority={})",
                PrefixConfig.get().staffTrack(), PrefixConfig.get().prefixPriority());
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
                snapshot.lockedStaff(),
                snapshot.selected(),
                snapshot.availableGroups()
        );
        if (decision.clearOverride() && snapshot.selected() != null) {
            LuckPermsUsers.clearPrefixOverride(playerId).whenComplete((ignored, error) -> {
                if (error != null) {
                    LOGGER.warn("Failed to drop prefix override for {}", playerId, error);
                    return;
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
                minecraftServer.execute(() -> callback.accept(PrefixSelectionRules.Decision.unavailable(requestedGroup)));
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
                ? PrefixSelectionRules.decideAuto(snapshot.lockedStaff())
                : PrefixSelectionRules.decideSet(snapshot.lockedStaff(), snapshot.availableGroups(), requestedGroup);
        CompletableFuture<Void> mutation = switch (decision.kind()) {
            case STAFF_LOCKED, AUTO -> LuckPermsUsers.clearPrefixOverride(playerId);
            case APPLIED -> {
                PrefixOption option = snapshot.option(decision.selectedGroup());
                if (option == null) {
                    yield CompletableFuture.completedFuture(null);
                }
                yield LuckPermsUsers.applyPrefixOverride(playerId, option.group(), option.prefixRaw());
            }
            case UNAVAILABLE, UNCHANGED -> CompletableFuture.completedFuture(null);
        };
        mutation.whenComplete((ignored, error) -> minecraftServer.execute(() -> {
            if (error != null) {
                LOGGER.warn("Prefix mutation failed for {}", playerId, error);
                callback.accept(PrefixSelectionRules.Decision.unavailable(requestedGroup));
                return;
            }
            if (decision.kind() != PrefixSelectionRules.Kind.UNAVAILABLE) {
                PrefixReportScheduler.request(minecraftServer, playerId);
            }
            callback.accept(decision);
        }));
    }

}
