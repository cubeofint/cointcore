package com.mawlee.cointcore.damage;

import com.mawlee.cointcore.CointCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class DamageGuardEvents {
    private DamageGuardEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (DamageGuard.shouldCancelIncoming(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (DamageGuard.shouldCancelIncoming(event.getEntity())) {
            event.setNewDamage(0.0F);
        }
    }
}
