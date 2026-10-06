package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.pvp.PvpModeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ClaimFlagEvents {
    private ClaimFlagEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.SPAWNER || !FtbIntegration.isAvailable()) {
            return;
        }

        Mob mob = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        FtbIntegration.getClaimTeamData(level, mob.blockPosition())
                .filter(teamData -> ClaimFlagService.blocksMobSpawn(teamData, mob.getType()))
                .ifPresent(ignored -> event.setSpawnCancelled(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!FtbIntegration.isAvailable() || !(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (attacker instanceof FakePlayer || FtbIntegration.hasBypassProtection(attacker)) {
            return;
        }

        if (event.getTarget() instanceof ServerPlayer target) {
            if (shouldBlockPlayerDamage(attacker, target)) {
                event.setCanceled(true);
            }
            return;
        }

        if (event.getTarget() instanceof LivingEntity living && !(living instanceof Player)
                && ClaimFlagService.blocksMobDamageFromPlayer(attacker, living)) {
            event.setCanceled(true);
            attacker.displayClientMessage(
                    CointCoreMessages.forPlayer(attacker, CointCoreMessages.CLAIM_MOB_DAMAGE_BLOCKED),
                    true
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!FtbIntegration.isAvailable() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer target) {
            ServerPlayer attacker = PvpModeManager.resolveAttackingPlayer(event.getSource());
            if (attacker != null && !(attacker instanceof FakePlayer) && !FtbIntegration.hasBypassProtection(attacker)) {
                if (shouldBlockPlayerDamage(attacker, target)) {
                    event.setCanceled(true);
                }
                return;
            }
            if (isMobDamage(event.getSource()) && blocksMobDamageHere(level, target.blockPosition())) {
                event.setCanceled(true);
            }
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) {
            return;
        }

        ServerPlayer attacker = PvpModeManager.resolveAttackingPlayer(event.getSource());
        if (attacker == null || attacker instanceof FakePlayer || FtbIntegration.hasBypassProtection(attacker)) {
            return;
        }
        if (ClaimFlagService.blocksMobDamageFromPlayer(attacker, living)) {
            event.setCanceled(true);
            attacker.displayClientMessage(
                    CointCoreMessages.forPlayer(attacker, CointCoreMessages.CLAIM_MOB_DAMAGE_BLOCKED),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!FtbIntegration.isAvailable()
                || !(event.getRayTraceResult() instanceof EntityHitResult hitResult)
                || !(event.getProjectile().getOwner() instanceof ServerPlayer attacker)
                || FtbIntegration.hasBypassProtection(attacker)) {
            return;
        }

        if (hitResult.getEntity() instanceof ServerPlayer target && shouldBlockPlayerDamage(attacker, target)) {
            event.setCanceled(true);
            return;
        }

        if (hitResult.getEntity() instanceof LivingEntity living
                && !(living instanceof Player)
                && ClaimFlagService.blocksMobDamageFromPlayer(attacker, living)) {
            event.setCanceled(true);
            attacker.displayClientMessage(
                    CointCoreMessages.forPlayer(attacker, CointCoreMessages.CLAIM_MOB_DAMAGE_BLOCKED),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!event.getNewState().is(Blocks.FIRE)) {
            return;
        }
        if (ClaimFlagService.blocksFireSpread(level, event.getPos())) {
            event.setNewState(event.getOriginalState());
        }
    }

    @SubscribeEvent
    public static void onIgnite(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getFace() == null) {
            return;
        }
        if (!event.getItemStack().is(Items.FLINT_AND_STEEL) && !event.getItemStack().is(Items.FIRE_CHARGE)) {
            return;
        }
        BlockPos firePos = event.getPos().relative(event.getFace());
        if (!ClaimFlagService.blocksFireSpread(player.serverLevel(), firePos)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.CLAIM_FIRE_BLOCKED), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClaimEntryGuard.onTick(player);
        }
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        ClaimEntryGuard.onTeleport(event);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ClaimEntryGuard.forget(event.getEntity().getUUID());
    }

    private static boolean shouldBlockPlayerDamage(ServerPlayer attacker, ServerPlayer target) {
        if (attacker.getUUID().equals(target.getUUID())) {
            return false;
        }

        var targetClaim = FtbIntegration.getClaimTeamData(target.serverLevel(), target.blockPosition());
        if (targetClaim.isPresent() && ClaimFlagService.blocksPvp(targetClaim.get())) {
            return true;
        }

        var attackerClaim = FtbIntegration.getClaimTeamData(attacker.serverLevel(), attacker.blockPosition());
        return attackerClaim.isPresent() && ClaimFlagService.blocksPvp(attackerClaim.get());
    }

    private static boolean blocksMobDamageHere(ServerLevel level, BlockPos pos) {
        return FtbIntegration.getClaimTeamData(level, pos)
                .map(ClaimFlagService::blocksMobDamageToPlayer)
                .orElse(false);
    }

    private static boolean isMobDamage(DamageSource source) {
        if (PvpModeManager.resolveAttackingPlayer(source) != null) {
            return false;
        }
        Entity cause = source.getEntity();
        if (cause instanceof Player) {
            return false;
        }
        if (cause instanceof LivingEntity) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof LivingEntity && !(direct instanceof Player);
    }
}
