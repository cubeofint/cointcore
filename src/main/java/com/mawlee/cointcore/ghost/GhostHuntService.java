package com.mawlee.cointcore.ghost;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Ghost hunt: right-clicking a marked block or entity counts it once per player and tells only that player
 * "Найдено N/X". Staff see who found how many with {@code /ghost top}.
 */
public final class GhostHuntService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double REACH = 8.0D;

    private GhostHuntService() {
    }

    public static boolean canManage(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.GHOST_MANAGE);
    }

    public static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    public static Optional<GhostHuntSavedData.Ghost> ghostAt(ServerLevel level, BlockPos pos) {
        return GhostHuntSavedData.get(level.getServer()).atBlock(dimensionId(level), pos.asLong());
    }

    public static Optional<GhostHuntSavedData.Ghost> ghostOf(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return GhostHuntSavedData.get(level.getServer()).ofEntity(entity.getUUID());
    }

    /** A player clicked a ghost. */
    public static void onFound(ServerPlayer player, GhostHuntSavedData.Ghost ghost) {
        GhostHuntSavedData data = GhostHuntSavedData.get(player.server);
        boolean added = data.markFound(player.getUUID(), player.getGameProfile().getName(), ghost.id());
        int found = data.foundCount(player.getUUID());
        int total = data.total();
        if (!added) {
            player.displayClientMessage(
                    CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_ALREADY, found, total), true);
            return;
        }
        String name = player.getGameProfile().getName();
        LOGGER.info("GHOST {} found #{} ({}/{})", name, ghost.id(), found, total);
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_FOUND, found, total));
        player.playNotifySound(SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 1.0F, 0.6F);
        String label = ghost.label() == null || ghost.label().isEmpty() ? "" : " \"" + ghost.label() + "\"";
        notifyStaff(player, CointCoreMessages.GHOST_STAFF_FOUND, name, ghost.id(), label, found, total);
        if (found >= total) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_ALL_FOUND, found, total));
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
            LOGGER.info("GHOST {} found all {} ghosts", name, total);
            notifyStaff(player, CointCoreMessages.GHOST_STAFF_ALL_FOUND, name, total);
        }
    }

    /** Tells online staff with {@code cointcore.ghost.manage} (not the finder) about a find. */
    private static void notifyStaff(ServerPlayer finder, String key, Object... args) {
        for (ServerPlayer staff : finder.server.getPlayerList().getPlayers()) {
            if (staff != finder && canManage(staff)) {
                staff.sendSystemMessage(CointCoreMessages.forPlayer(staff, key, args));
            }
        }
    }

    /** Marks what the player looks at: an entity within reach first, else the block. */
    public static Optional<GhostHuntSavedData.Ghost> addLookedAt(ServerPlayer player, String label) {
        ServerLevel level = player.serverLevel();
        GhostHuntSavedData data = GhostHuntSavedData.get(player.server);
        Entity entity = lookedAtEntity(player);
        if (entity != null) {
            if (data.ofEntity(entity.getUUID()).isPresent()) {
                return Optional.empty();
            }
            entity.setInvulnerable(true);
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            return Optional.of(data.addEntity(dimensionId(level), entity.blockPosition().asLong(), entity.getUUID(), label));
        }
        BlockPos pos = lookedAtBlock(player);
        if (pos == null || data.atBlock(dimensionId(level), pos.asLong()).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(data.addBlock(dimensionId(level), pos.asLong(), label));
    }

    public static Optional<GhostHuntSavedData.Ghost> lookedAtGhost(ServerPlayer player) {
        Entity entity = lookedAtEntity(player);
        if (entity != null) {
            Optional<GhostHuntSavedData.Ghost> ghost = ghostOf(entity);
            if (ghost.isPresent()) {
                return ghost;
            }
        }
        BlockPos pos = lookedAtBlock(player);
        return pos == null ? Optional.empty() : ghostAt(player.serverLevel(), pos);
    }

    private static Entity lookedAtEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(REACH));
        AABB box = player.getBoundingBox().expandTowards(player.getViewVector(1.0F).scale(REACH)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                candidate -> !candidate.isSpectator() && candidate != player, REACH * REACH);
        if (hit == null) {
            return null;
        }
        BlockPos block = lookedAtBlock(player);
        if (block != null && eye.distanceToSqr(Vec3.atCenterOf(block)) < eye.distanceToSqr(hit.getLocation())) {
            return null;
        }
        return hit.getEntity();
    }

    private static BlockPos lookedAtBlock(ServerPlayer player) {
        HitResult hit = player.pick(REACH, 1.0F, false);
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK ? blockHit.getBlockPos() : null;
    }
}
