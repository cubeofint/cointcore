package com.mawlee.cointcore.pvp;

import com.mawlee.cointcore.CointCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class PvpModeEvents {
    private PvpModeEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        if (!PvpModeManager.canPlayerVsPlayerFight(attacker, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        ServerPlayer attacker = PvpModeManager.resolveAttackingPlayer(event.getSource());
        if (attacker == null || attacker.getUUID().equals(victim.getUUID())) {
            return;
        }

        if (!PvpModeManager.canPlayerVsPlayerFight(attacker, victim)) {
            event.setCanceled(true);
            return;
        }

        PvpModeManager.recordPlayerDamage(victim);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) {
            return;
        }

        if (!(hitResult.getEntity() instanceof ServerPlayer target)) {
            return;
        }

        if (!(event.getProjectile().getOwner() instanceof ServerPlayer shooter)) {
            return;
        }

        if (!PvpModeManager.canPlayerVsPlayerFight(shooter, target)) {
            event.setCanceled(true);
        }
    }
}
