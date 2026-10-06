package com.mawlee.cointcore.explosion;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.ExplosionTerrainConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.Explosion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Strips block destruction from vanilla-style explosions and, by default,
 * TNT damage to players.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ExplosionTerrainEvents {
    private ExplosionTerrainEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (ExplosionTerrainConfig.isEnabled()) {
            event.getAffectedBlocks().clear();
        }
        if (!ExplosionTerrainConfig.isTntPlayerDamageEnabled() && isTntExplosion(event.getExplosion())) {
            event.getAffectedEntities().removeIf(entity -> entity instanceof Player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ExplosionTerrainConfig.isTntPlayerDamageEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        if (isTntDamage(event.getSource())) {
            event.setCanceled(true);
        }
    }

    private static boolean isTntExplosion(Explosion explosion) {
        return isTntEntity(explosion.getDirectSourceEntity());
    }

    private static boolean isTntDamage(DamageSource source) {
        return isTntEntity(source.getDirectEntity()) || isTntEntity(source.getEntity());
    }

    private static boolean isTntEntity(Entity entity) {
        return entity instanceof PrimedTnt || entity instanceof MinecartTNT;
    }
}
