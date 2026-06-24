package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.FtbIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class ClaimGuard {
    private static final double NEAREST_ACTOR_RANGE_SQR = 64.0 * 64.0;

    private ClaimGuard() {
    }

    public static boolean isAvailable() {
        return FtbIntegration.isAvailable();
    }

    public static boolean canEdit(Entity actor, Level level, BlockPos pos) {
        return FtbIntegration.canEditBlock(actor, level, pos);
    }

    public static boolean canInteract(Entity actor, Level level, BlockPos pos) {
        return FtbIntegration.canInteractBlock(actor, level, pos);
    }

    public static boolean canEditBox(Entity actor, Level level, AABB box) {
        return FtbIntegration.canEditBox(actor, level, box);
    }

    public static void denyUnlessCanEdit(Entity actor, Level level, BlockPos pos) {
        if (!canEdit(actor, level, pos)) {
            throw new ClaimDeniedException(pos);
        }
    }

    public static boolean canEditAtCurrentActor(Level level, BlockPos pos, Optional<UUID> controllingPlayer) {
        Entity actor = resolveActor(level, pos, controllingPlayer);
        if (actor == null) {
            return true;
        }
        return canEdit(actor, level, pos);
    }

    public static Entity resolveActor(Level level, BlockPos contextPos, Optional<UUID> controllingPlayer) {
        return resolveActorUuid(level, contextPos, controllingPlayer)
                .flatMap(uuid -> resolveEntity(level, uuid))
                .orElse(null);
    }

    public static Optional<UUID> resolveActorUuid(Level level, BlockPos contextPos, Optional<UUID> controllingPlayer) {
        Optional<UUID> fromContext = ClaimGuardContext.currentActor();
        if (fromContext.isPresent()) {
            return fromContext;
        }

        if (controllingPlayer.isPresent()) {
            return controllingPlayer;
        }

        if (level instanceof ServerLevel serverLevel && contextPos != null) {
            return findNearestPlayerUuid(serverLevel, contextPos);
        }

        return Optional.empty();
    }

    public static Optional<Entity> resolveEntity(Level level, UUID playerId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayer(playerId);
        if (online != null) {
            return Optional.of(online);
        }

        return Optional.empty();
    }

    public static Optional<UUID> resolveMinecartAssemblerActor(ServerLevel level, BlockPos assemblerPos) {
        return findNearestPlayerUuid(level, assemblerPos);
    }

    public static boolean shouldGuardBlockMutations() {
        return isAvailable() && ClaimGuardContext.currentActor().isPresent();
    }

    /**
     * Entangled proxy without player: allow only if bound chunk belongs to the same FTB team as the entangled block.
     */
    public static boolean canEntangledProxy(Level sourceLevel, BlockPos sourcePos, Level boundLevel, BlockPos boundPos) {
        if (!isAvailable()) {
            return true;
        }
        if (!(sourceLevel instanceof ServerLevel source) || !(boundLevel instanceof ServerLevel bound)) {
            return true;
        }

        var boundClaim = FtbIntegration.getClaimTeamData(bound, boundPos);
        if (boundClaim.isEmpty()) {
            return true;
        }

        var sourceClaim = FtbIntegration.getClaimTeamData(source, sourcePos);
        if (sourceClaim.isEmpty()) {
            return false;
        }

        return sourceClaim.get().getTeam().getTeamId().equals(boundClaim.get().getTeam().getTeamId());
    }

    public static boolean canAccessEntangledBound(
            Level sourceLevel,
            BlockPos sourcePos,
            Level boundLevel,
            BlockPos boundPos
    ) {
        Optional<UUID> interactor = ClaimGuardContext.currentInteractor();
        if (interactor.isPresent()) {
            Entity actor = resolveEntity(sourceLevel, interactor.get()).orElse(null);
            return actor != null && canInteract(actor, boundLevel, boundPos);
        }

        return canEntangledProxy(sourceLevel, sourcePos, boundLevel, boundPos);
    }

    private static Optional<UUID> findNearestPlayerUuid(ServerLevel level, BlockPos pos) {
        ServerPlayer nearest = null;
        double nearestDistance = NEAREST_ACTOR_RANGE_SQR;
        Vec3 center = Vec3.atCenterOf(pos);

        for (ServerPlayer player : level.players()) {
            double distance = player.position().distanceToSqr(center);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest == null ? Optional.empty() : Optional.of(nearest.getUUID());
    }
}
