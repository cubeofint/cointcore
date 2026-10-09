package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimEntryGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<RelativeMovement> KEEP_ROTATION = EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT);

    private static final Set<UUID> EJECTING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_EJECT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, SafePos> LAST_SAFE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE_TICK = new ConcurrentHashMap<>();

    private ClaimEntryGuard() {
    }

    public static void forget(UUID playerId) {
        LAST_SAFE.remove(playerId);
        LAST_MESSAGE_TICK.remove(playerId);
        EJECTING.remove(playerId);
        PENDING_EJECT.remove(playerId);
    }

    /**
     * Runs inside {@code ServerPlayer.doTick()}. Vanilla moves the player back to the position it had at the start
     * of the connection tick right after {@code doTick()}, so a teleport issued here is undone on the server while
     * the client keeps waiting for it. The eject is therefore deferred to {@link #flushPendingEjects}.
     */
    public static void onTick(ServerPlayer player) {
        if (!FtbIntegration.isAvailable() || EJECTING.contains(player.getUUID()) || !player.isAlive() || player.isSpectator()) {
            return;
        }

        if (isAllowed(player, player.serverLevel(), player.blockPosition())) {
            LAST_SAFE.put(player.getUUID(), SafePos.from(player));
            return;
        }

        PENDING_EJECT.add(player.getUUID());
    }

    public static void flushPendingEjects(MinecraftServer server) {
        if (PENDING_EJECT.isEmpty()) {
            return;
        }
        for (UUID playerId : Set.copyOf(PENDING_EJECT)) {
            PENDING_EJECT.remove(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (isAllowed(player, player.serverLevel(), player.blockPosition())) {
                continue;
            }
            eject(player);
        }
    }

    public static void onTeleport(EntityTeleportEvent event) {
        if (!FtbIntegration.isAvailable() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (EJECTING.contains(player.getUUID()) || player.isSpectator()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos destination = BlockPos.containing(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        if (isAllowed(player, level, destination)) {
            return;
        }

        event.setCanceled(true);
        notifyDenied(player);
    }

    private static boolean isAllowed(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return FtbIntegration.getClaimTeamData(level, pos)
                .map(teamData -> !ClaimFlagService.deniesEntry(teamData, player))
                .orElse(true);
    }

    private static void eject(ServerPlayer player) {
        if (!EJECTING.add(player.getUUID())) {
            return;
        }
        try {
            if (player.isPassenger()) {
                player.stopRiding();
            }

            SafePos safe = LAST_SAFE.get(player.getUUID());
            if (safe != null) {
                ServerLevel level = player.getServer().getLevel(safe.dimension());
                if (level != null && isAllowed(player, level, BlockPos.containing(safe.x(), safe.y(), safe.z()))) {
                    teleportKeepingRotation(player, level, safe.x(), safe.y(), safe.z());
                    notifyDenied(player);
                    return;
                }
            }

            if (teleportToSpawnIfAllowed(player, player.serverLevel())
                    || teleportToSpawnIfAllowed(player, player.getServer().overworld())) {
                notifyDenied(player);
                return;
            }

            LOGGER.warn(
                    "Claim entry guard: no allowed position to eject {} from {} at {}; world spawn is inside a members-only claim",
                    player.getGameProfile().getName(),
                    player.level().dimension().location(),
                    player.blockPosition().toShortString()
            );
        } finally {
            EJECTING.remove(player.getUUID());
        }
    }

    private static boolean teleportToSpawnIfAllowed(ServerPlayer player, ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        if (!isAllowed(player, level, spawn)) {
            return false;
        }
        teleportKeepingRotation(player, level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        return true;
    }

    private static void teleportKeepingRotation(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        player.teleportTo(level, x, y, z, KEEP_ROTATION, player.getYRot(), player.getXRot());
    }

    private static void notifyDenied(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_MESSAGE_TICK.get(player.getUUID());
        if (last != null && now - last < 40L) {
            return;
        }
        LAST_MESSAGE_TICK.put(player.getUUID(), now);
        player.displayClientMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.CLAIM_ENTRY_DENIED), true);
    }

    private record SafePos(ResourceKey<Level> dimension, double x, double y, double z) {
        private static SafePos from(ServerPlayer player) {
            return new SafePos(player.level().dimension(), player.getX(), player.getY(), player.getZ());
        }
    }
}
