package com.mawlee.cointcore.item;

import com.mawlee.cointcore.config.ItemPerfConfig;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-tick budget for {@link net.minecraft.world.Containers#dropItemStack}. Breaking a storage block
 * holding millions of items would otherwise spawn hundreds of thousands of 10–30 item entities in one
 * tick. Past a soft budget drops spawn as full stacks (which skip merge scans); past the hard budget
 * the rest becomes {@link ItemPiles item pile} entities at the end of the tick.
 */
public final class MassDropGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double VANILLA_MOTION_SPREAD = 0.11485000171139836;
    private static final int VANILLA_AVERAGE_SPLIT = 20;
    /** Pile reserves live only in entity NBT; keep each well under the 1 MiB region sector limit. */
    private static final int MAX_PILE_BYTES = 256 * 1024;

    private static final Map<ServerLevel, LevelBudget> BUDGETS = new IdentityHashMap<>();

    private MassDropGuard() {
    }

    /**
     * @return {@code true} if the drop was handled here and vanilla splitting must be skipped
     */
    public static boolean handle(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (!ItemPerfConfig.massDropGuard() || stack.isEmpty() || !level.getServer().isSameThread()) {
            return false;
        }
        LevelBudget budget = BUDGETS.computeIfAbsent(level, ignored -> new LevelBudget());
        long posKey = BlockPos.asLong(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        int expected = (stack.getCount() + VANILLA_AVERAGE_SPLIT - 1) / VANILLA_AVERAGE_SPLIT;
        if (budget.spawnedAt(posKey) + expected <= ItemPerfConfig.massDropSoftEntitiesPerPos()
                && budget.spawned + expected <= ItemPerfConfig.massDropMaxEntitiesPerLevelTick()) {
            budget.record(posKey, expected);
            return false;
        }
        dropBudgeted(level, budget, posKey, x, y, z, stack);
        return true;
    }

    private static void dropBudgeted(ServerLevel level, LevelBudget budget, long posKey, double x, double y, double z, ItemStack stack) {
        double width = EntityType.ITEM.getWidth();
        double spread = 1.0 - width;
        double half = width / 2.0;
        double px = Math.floor(x) + level.random.nextDouble() * spread + half;
        double py = Math.floor(y) + level.random.nextDouble() * spread;
        double pz = Math.floor(z) + level.random.nextDouble() * spread + half;
        int maxStack = Math.max(1, stack.getMaxStackSize());
        int perPosLimit = ItemPerfConfig.massDropMaxEntitiesPerPos();
        int levelLimit = ItemPerfConfig.massDropMaxEntitiesPerLevelTick();
        while (!stack.isEmpty()) {
            if (budget.spawnedAt(posKey) >= perPosLimit || budget.spawned >= levelLimit) {
                budget.overflow(posKey, x, y, z).add(stack, stack.getCount());
                stack.setCount(0);
                return;
            }
            ItemEntity entity = new ItemEntity(level, px, py, pz, stack.split(maxStack));
            entity.setDeltaMovement(
                    level.random.triangle(0.0, VANILLA_MOTION_SPREAD),
                    level.random.triangle(0.2, VANILLA_MOTION_SPREAD),
                    level.random.triangle(0.0, VANILLA_MOTION_SPREAD)
            );
            level.addFreshEntity(entity);
            budget.record(posKey, 1);
        }
    }

    /** Spawns pending piles and resets budgets; call once at the end of every server tick. */
    public static void flush() {
        if (BUDGETS.isEmpty()) {
            return;
        }
        List<Map.Entry<ServerLevel, LevelBudget>> pending = new ArrayList<>(BUDGETS.entrySet());
        BUDGETS.clear();
        for (Map.Entry<ServerLevel, LevelBudget> entry : pending) {
            ServerLevel level = entry.getKey();
            for (Overflow overflow : entry.getValue().overflows.values()) {
                spawnPiles(level, overflow);
            }
        }
    }

    private static void spawnPiles(ServerLevel level, Overflow overflow) {
        if (overflow.entries.isEmpty()) {
            return;
        }
        int maxEntries = ItemPerfConfig.massDropPileMaxEntries();
        List<ItemPile.Entry> current = new ArrayList<>();
        int currentBytes = 0;
        int piles = 0;
        long items = 0L;
        for (ItemPile.Entry entry : overflow.entries) {
            int bytes = ItemPile.encodeEntry(level.registryAccess(), entry).sizeInBytes();
            if (!current.isEmpty() && (current.size() >= maxEntries || currentBytes + bytes > MAX_PILE_BYTES)) {
                ItemPiles.spawn(level, overflow.x, overflow.y, overflow.z, new ItemPile(current), 0);
                piles++;
                current = new ArrayList<>();
                currentBytes = 0;
            }
            current.add(entry);
            currentBytes += bytes;
            items += entry.count();
        }
        ItemPiles.spawn(level, overflow.x, overflow.y, overflow.z, new ItemPile(current), 0);
        piles++;
        LOGGER.info(
                "Mass drop guard in {} at {} {} {}: packed {} items ({} kinds) into {} pile(s)",
                level.dimension().location(),
                Mth.floor(overflow.x),
                Mth.floor(overflow.y),
                Mth.floor(overflow.z),
                items,
                overflow.entries.size(),
                piles
        );
    }

    private static final class LevelBudget {
        private final Long2IntOpenHashMap spawnedByPos = new Long2IntOpenHashMap();
        private final Long2ObjectLinkedOpenHashMap<Overflow> overflows = new Long2ObjectLinkedOpenHashMap<>();
        private int spawned;

        int spawnedAt(long posKey) {
            return spawnedByPos.get(posKey);
        }

        void record(long posKey, int entities) {
            spawnedByPos.addTo(posKey, entities);
            spawned += entities;
        }

        Overflow overflow(long posKey, double x, double y, double z) {
            Overflow overflow = overflows.get(posKey);
            if (overflow == null) {
                overflow = new Overflow(x, y, z);
                overflows.put(posKey, overflow);
            }
            return overflow;
        }
    }

    private static final class Overflow {
        private final double x;
        private final double y;
        private final double z;
        private final List<ItemPile.Entry> entries = new ArrayList<>();
        private final Int2ObjectOpenHashMap<List<Integer>> indexByHash = new Int2ObjectOpenHashMap<>();

        Overflow(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        void add(ItemStack stack, long count) {
            int hash = ItemStack.hashItemAndComponents(stack);
            List<Integer> bucket = indexByHash.computeIfAbsent(hash, ignored -> new ArrayList<>(1));
            for (int index : bucket) {
                ItemPile.Entry existing = entries.get(index);
                if (ItemStack.isSameItemSameComponents(existing.proto(), stack)) {
                    existing.grow(count);
                    return;
                }
            }
            bucket.add(entries.size());
            entries.add(new ItemPile.Entry(stack, count));
        }
    }
}
