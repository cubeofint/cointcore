package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimEntryGuard {
    private static final Set<UUID> EJECTING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, SafePos> LAST_SAFE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE_TICK = new ConcurrentHashMap<>();

    private ClaimEntryGuard() {
    }

    public static void forget(UUID playerId) {
        LAST_SAFE.remove(playerId);
        LAST_MESSAGE_TICK.remove(playerId);
        EJECTING.remove(playerId);
    }

    public static void onTick(ServerPlayer player) {
        if (!FtbIntegration.isAvailable() || EJECTING.contains(player.getUUID()) || !player.isAlive() || player.isSpectator()) {
            return;
        }

        if (isAllowed(player, player.serverLevel(), player.blockPosition())) {
            LAST_SAFE.put(player.getUUID(), SafePos.from(player));
            return;
        }

        eject(player);
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
            SafePos safe = LAST_SAFE.get(player.getUUID());
            if (player.isPassenger()) {
                player.stopRiding();
            }
            if (safe != null) {
                ServerLevel level = player.getServer().getLevel(safe.dimension());
                if (level != null && isAllowed(player, level, BlockPos.containing(safe.x(), safe.y(), safe.z()))) {
                    player.teleportTo(level, safe.x(), safe.y(), safe.z(), EnumSet.noneOf(RelativeMovement.class), safe.yRot(), safe.xRot());
                    notifyDenied(player);
                    return;
                }
            }

            ServerLevel level = player.serverLevel();
            BlockPos spawn = level.getSharedSpawnPos();
            player.teleportTo(
                    level,
                    spawn.getX() + 0.5,
                    spawn.getY(),
                    spawn.getZ() + 0.5,
                    EnumSet.noneOf(RelativeMovement.class),
                    level.getSharedSpawnAngle(),
                    0.0F
            );
            notifyDenied(player);
        } finally {
            EJECTING.remove(player.getUUID());
        }
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

    private record SafePos(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
        private static SafePos from(ServerPlayer player) {
            return new SafePos(
                    player.level().dimension(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }
}
