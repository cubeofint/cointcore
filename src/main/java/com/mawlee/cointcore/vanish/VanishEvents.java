package com.mawlee.cointcore.vanish;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.CointCoreRuntimeCleanup;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class VanishEvents {
    private VanishEvents() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CointCoreRuntimeCleanup.onServerStopped();
    }

    @SubscribeEvent
    public static void onInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            VanishInteractionTracker.updateBlockInteraction(player, event.getHitVec());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            VanishInteractionTracker.updateBlockInteraction(player, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            VanishInteractionTracker.updateEntityInteraction(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            VanishInteractionTracker.updateEntityInteraction(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player
                && VanishManager.isVanished(player)
                && event.getEffectInstance().getEffect().is(MobEffects.GLOWING)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player
                && VanishManager.isVanished(player)
                && event.getEffectInstance().getEffect().is(MobEffects.GLOWING)) {
            player.removeEffect(MobEffects.GLOWING);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !VanishManager.isVanished(player)) {
            return;
        }

        if (player.hasEffect(MobEffects.GLOWING) || player.hasGlowingTag()) {
            player.removeEffect(MobEffects.GLOWING);
            player.setGlowingTag(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer target
                && VanishManager.isVanished(target)
                && !VanishManager.canMobsTarget(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) {
            return;
        }

        if (hitResult.getEntity() instanceof ServerPlayer target && VanishManager.isVanished(target)) {
            var owner = event.getProjectile().getOwner();
            if (!(owner instanceof ServerPlayer viewer) || VanishManager.shouldHideFrom(target, viewer)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onVanillaGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof ServerPlayer player && VanishManager.isVanished(player)) {
            event.setCanceled(true);
        }
    }
}
