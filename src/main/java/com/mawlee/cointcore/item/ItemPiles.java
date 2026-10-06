package com.mawlee.cointcore.item;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mawlee.cointcore.mixin.accessor.ItemEntityAgeAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Item pile entities: a regular {@link ItemEntity} showing one stack with an {@link ItemPile} reserve.
 * Taking the whole display stack (player, hopper, merge) leaves an empty stack on discard, and the next
 * stack respawns in place. Any other removal (cleanup, fire, lava, explosion, void, despawn) destroys
 * the reserve together with the entity.
 */
public final class ItemPiles {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAME_LANG = "ru_ru";

    private static final List<Successor> SUCCESSORS = new ArrayList<>();

    private ItemPiles() {
    }

    private record Successor(ServerLevel level, double x, double y, double z, ItemPile pile, int age) {
    }

    public static void spawn(ServerLevel level, double x, double y, double z, ItemPile pile, int age) {
        ItemStack display = pile.takeChunk();
        if (display.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, x, y, z, display, 0.0, 0.0, 0.0);
        ((ItemEntityAgeAccessor) entity).cointcore$setAge(age);
        if (!pile.isEmpty()) {
            ((ItemPileHolder) entity).cointcore$setPile(pile);
            refreshName(entity, pile);
        }
        level.addFreshEntity(entity);
    }

    /** Server tick head of a pile entity: refill a partially taken display stack and keep the label current. */
    public static void tick(ItemEntity entity) {
        ItemPileHolder holder = (ItemPileHolder) entity;
        ItemPile pile = holder.cointcore$getPile();
        if (pile == null || entity.level().isClientSide) {
            return;
        }
        ItemStack display = entity.getItem();
        if (!display.isEmpty() && display.getCount() < display.getMaxStackSize() && pile.topUp(display) > 0) {
            entity.setItem(display);
        }
        if (pile.isEmpty()) {
            holder.cointcore$setPile(null);
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            return;
        }
        refreshName(entity, pile);
    }

    public static void onRemoved(ItemEntity entity) {
        ItemPileHolder holder = (ItemPileHolder) entity;
        ItemPile pile = holder.cointcore$getPile();
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (pile == null || pile.isEmpty() || reason == null || !reason.shouldDestroy()) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        holder.cointcore$setPile(null);
        if (reason == Entity.RemovalReason.DISCARDED && entity.getItem().isEmpty()) {
            int age = ((ItemEntityAgeAccessor) entity).cointcore$getAge();
            SUCCESSORS.add(new Successor(level, entity.getX(), entity.getY(), entity.getZ(), pile, age));
            return;
        }
        LOGGER.info(
                "Item pile destroyed in {} at {} {} {}: {} items",
                level.dimension().location(),
                entity.getBlockX(),
                entity.getBlockY(),
                entity.getBlockZ(),
                pile.total() + entity.getItem().getCount()
        );
    }

    /** Respawn the next stack of piles whose display stack was taken this tick. */
    public static void flush() {
        if (SUCCESSORS.isEmpty()) {
            return;
        }
        List<Successor> pending = new ArrayList<>(SUCCESSORS);
        SUCCESSORS.clear();
        for (Successor successor : pending) {
            spawn(successor.level, successor.x, successor.y, successor.z, successor.pile, successor.age);
        }
    }

    public static void clear() {
        SUCCESSORS.clear();
    }

    private static void refreshName(ItemEntity entity, ItemPile pile) {
        ItemPileHolder holder = (ItemPileHolder) entity;
        long total = pile.total() + entity.getItem().getCount();
        if (holder.cointcore$getShownTotal() == total) {
            return;
        }
        holder.cointcore$setShownTotal(total);
        String template = CointCoreMessages.translateKey(NAME_LANG, CointCoreMessages.ITEM_PILE_NAME);
        entity.setCustomName(LegacyTextParser.parse(String.format(template, total)));
        entity.setCustomNameVisible(true);
    }
}
