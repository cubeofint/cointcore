package com.mawlee.cointcore.ghost;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Right-click on a ghost counts it (and does nothing else, so a ghost chest does not open); ghosts cannot be broken
 * or hit except by staff. Staff sneaking interact normally so they can set ghosts up. The click handlers also take
 * events a claim mod (FTB Chunks) already cancelled, so ghosts inside someone's claim can still be found.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class GhostHuntEvents {
    private GhostHuntEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GhostHuntService.ghostAt(level, event.getPos()).ifPresent(ghost -> {
            if (staffEditing(player)) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                GhostHuntService.onFound(player, ghost);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleEntity(event, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (handleEntity(event, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** True when the target is a ghost and the click must be swallowed. */
    private static boolean handleEntity(PlayerInteractEvent event, Entity target) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        GhostHuntSavedData.Ghost ghost = GhostHuntService.ghostOf(target).orElse(null);
        if (ghost == null || staffEditing(player)) {
            return false;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            // Armor stands fire both EntityInteractSpecific and EntityInteract for one click.
            long tick = player.serverLevel().getGameTime();
            Long previous = LAST_ENTITY_CLICK.put(player.getUUID(), tick);
            if (previous == null || previous != tick) {
                GhostHuntService.onFound(player, ghost);
            }
        }
        return true;
    }

    private static final java.util.Map<java.util.UUID, Long> LAST_ENTITY_CLICK = new java.util.concurrent.ConcurrentHashMap<>();

    /** LOWEST and not on cancelled events: a staff break removes the ghost only if the block really goes. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        GhostHuntService.ghostAt(level, event.getPos()).ifPresent(ghost -> {
            if (GhostHuntService.canManage(player)) {
                GhostHuntSavedData.get(level.getServer()).remove(ghost.id());
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.GHOST_REMOVED, ghost.id()));
            } else {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (GhostHuntService.ghostOf(event.getTarget()).isPresent() && !GhostHuntService.canManage(player)) {
            event.setCanceled(true);
        }
    }

    private static boolean staffEditing(ServerPlayer player) {
        return player.isShiftKeyDown() && GhostHuntService.canManage(player);
    }
}
