package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.pvp.PvpModeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ClaimFlagEvents {
    private ClaimFlagEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.SPAWNER) {
            return;
        }

        if (!FtbIntegration.isAvailable()) {
            return;
        }

        Mob mob = event.getEntity();
        if (mob.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        FtbIntegration.getClaimTeamData(level, mob.blockPosition())
                .filter(ClaimFlagService::blocksHostileMobSpawn)
                .ifPresent(ignored -> event.setSpawnCancelled(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!FtbIntegration.isAvailable() || !(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (FtbIntegration.hasBypassProtection(attacker)) {
            return;
        }

        if (event.getTarget() instanceof ServerPlayer target) {
            handlePlayerTarget(attacker, target, event);
            return;
        }

        if (event.getTarget() instanceof LivingEntity living && !(living instanceof Player)) {
            handleMobTarget(attacker, living, event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!FtbIntegration.isAvailable() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer attacker = PvpModeManager.resolveAttackingPlayer(event.getSource());
        if (attacker == null || FtbIntegration.hasBypassProtection(attacker)) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer target) {
            handlePlayerTarget(attacker, target, event);
            return;
        }

        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            handleMobTarget(attacker, living, event);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!FtbIntegration.isAvailable()
                || !(event.getRayTraceResult() instanceof EntityHitResult hitResult)
                || !(event.getProjectile().getOwner() instanceof ServerPlayer attacker)) {
            return;
        }

        if (FtbIntegration.hasBypassProtection(attacker)) {
            return;
        }

        if (hitResult.getEntity() instanceof ServerPlayer target) {
            handlePlayerTarget(attacker, target, event);
            return;
        }

        if (hitResult.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            handleMobTarget(attacker, living, event);
        }
    }

    private static void handlePlayerTarget(ServerPlayer attacker, ServerPlayer target, AttackEntityEvent event) {
        if (shouldBlockPlayerDamage(attacker, target)) {
            event.setCanceled(true);
        }
    }

    private static void handlePlayerTarget(ServerPlayer attacker, ServerPlayer target, LivingIncomingDamageEvent event) {
        if (shouldBlockPlayerDamage(attacker, target)) {
            event.setCanceled(true);
        }
    }

    private static void handlePlayerTarget(ServerPlayer attacker, ServerPlayer target, ProjectileImpactEvent event) {
        if (shouldBlockPlayerDamage(attacker, target)) {
            event.setCanceled(true);
        }
    }

    private static void handleMobTarget(ServerPlayer attacker, LivingEntity mob, AttackEntityEvent event) {
        if (shouldBlockMobDamage(attacker, mob)) {
            event.setCanceled(true);
            notifyMobProtected(attacker);
        }
    }

    private static void handleMobTarget(ServerPlayer attacker, LivingEntity mob, LivingIncomingDamageEvent event) {
        if (shouldBlockMobDamage(attacker, mob)) {
            event.setCanceled(true);
            notifyMobProtected(attacker);
        }
    }

    private static void handleMobTarget(ServerPlayer attacker, LivingEntity mob, ProjectileImpactEvent event) {
        if (shouldBlockMobDamage(attacker, mob)) {
            event.setCanceled(true);
            notifyMobProtected(attacker);
        }
    }

    private static boolean shouldBlockPlayerDamage(ServerPlayer attacker, ServerPlayer target) {
        if (attacker.getUUID().equals(target.getUUID())) {
            return false;
        }

        var targetClaim = FtbIntegration.getClaimTeamData(target.serverLevel(), target.blockPosition());
        if (targetClaim.isPresent() && ClaimFlagService.blocksPlayerDamage(targetClaim.get())) {
            return true;
        }

        var attackerClaim = FtbIntegration.getClaimTeamData(attacker.serverLevel(), attacker.blockPosition());
        return attackerClaim.isPresent() && ClaimFlagService.blocksPlayerDamage(attackerClaim.get());
    }

    private static boolean shouldBlockMobDamage(ServerPlayer attacker, LivingEntity mob) {
        return ClaimFlagService.blocksMobDamageFromPlayer(attacker, mob);
    }

    private static void notifyMobProtected(ServerPlayer attacker) {
        attacker.displayClientMessage(CointCoreMessages.forPlayer(attacker, CointCoreMessages.CLAIM_MOB_DAMAGE_BLOCKED), true);
    }
}
