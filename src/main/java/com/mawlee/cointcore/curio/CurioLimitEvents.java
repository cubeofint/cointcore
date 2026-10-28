package com.mawlee.cointcore.curio;

import com.mawlee.cointcore.config.CurioLimitConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.List;

/**
 * Enforces {@link CurioLimitConfig}: a limited item can't go into a Curios slot once the limit is worn, and copies that
 * got in anyway (shift-click paths, items worn before the limit existed) are moved back to the inventory.
 * Registered only when Curios is loaded.
 */
public final class CurioLimitEvents {
    private static final CurioValidationCache<ServerPlayer> VALIDATION = new CurioValidationCache<>();
    private CurioLimitEvents() {
    }

    public static void install() {
        NeoForge.EVENT_BUS.addListener(CurioLimitEvents::onCanEquip);
        NeoForge.EVENT_BUS.addListener(CurioLimitEvents::onChange);
        NeoForge.EVENT_BUS.addListener(CurioLimitEvents::onLogin);
    }

    private static void onCanEquip(CurioCanEquipEvent event) {
        SlotContext slot = event.getSlotContext();
        int limit = CurioLimitConfig.limitFor(event.getStack().getItem());
        if (limit < 0 || slot.cosmetic()) {
            return;
        }
        int others = 0;
        for (SlotResult worn : worn(slot.entity(), event.getStack().getItem())) {
            SlotContext at = worn.slotContext();
            if (!(at.identifier().equals(slot.identifier()) && at.index() == slot.index())) {
                others++;
            }
        }
        if (others >= limit) {
            event.setEquipResult(TriState.FALSE);
        }
    }

    private static void onChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getTo().isEmpty()
                && CurioLimitConfig.limitFor(event.getTo().getItem()) >= 0) {
            var before = event.getFrom();
            var after = event.getTo();
            if (!VALIDATION.needsValidation(player, CurioLimitConfig.getRevision(),
                    before.getItem() == after.getItem(), before.getCount(), after.getCount())) return;
            // Curios fires this from its own slot scan; change the slots after it finishes.
            player.server.execute(() -> enforce(player));
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            enforce(player);
        }
    }

    private static void enforce(ServerPlayer player) {
        if (player.isRemoved() || !CurioLimitConfig.isEnabled()) {
            return;
        }
        CurioLimitConfig.getLimits().forEach((id, limit) -> {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            List<SlotResult> worn = worn(player, item);
            for (int i = limit; i < worn.size(); i++) {
                SlotContext at = worn.get(i).slotContext();
                CuriosApi.getCuriosInventory(player)
                        .flatMap(handler -> handler.getStacksHandler(at.identifier()))
                        .ifPresent(stacks -> {
                            ItemStack removed = stacks.getStacks().getStackInSlot(at.index()).copy();
                            stacks.getStacks().setStackInSlot(at.index(), ItemStack.EMPTY);
                            if (!player.getInventory().add(removed)) {
                                player.drop(removed, false);
                            }
                        });
                player.displayClientMessage(Component.translatable("message.cointcore.curio_limit",
                        limit, item.getDefaultInstance().getHoverName()), true);
            }
        });
        VALIDATION.validated(player, CurioLimitConfig.getRevision());
    }

    /** Non-cosmetic Curios slots holding this item. */
    private static List<SlotResult> worn(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.findCurios(item).stream().filter(r -> !r.slotContext().cosmetic()).toList())
                .orElse(List.of());
    }
}
