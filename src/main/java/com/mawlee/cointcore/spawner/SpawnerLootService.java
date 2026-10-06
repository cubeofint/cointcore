package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.mixin.accessor.LivingEntityHurtByPlayerAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class SpawnerLootService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int emptyLootLogBudget = 8;

    private SpawnerLootService() {
    }

    public static boolean hasAdjacentStorageWithSpace(ServerLevel level, BlockPos spawnerPos) {
        for (Direction direction : Direction.values()) {
            IItemHandler handler = SpawnerAdjacentCaps.itemHandler(level, spawnerPos, direction);
            if (handler != null && hasFreeSpace(handler)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rolls the mob death loot table directly and inserts into adjacent handlers.
     * Overflow that does not fit is voided — never spawned as {@code ItemEntity}.
     */
    public static void generateAndInsert(ServerLevel level, BlockPos spawnerPos, Mob mob) {
        Player killer = createFakeKiller(level, spawnerPos);
        prepareMobForLoot(mob, killer);
        List<ItemStack> loot = rollDeathLoot(level, killer, mob);
        if (loot.isEmpty()) {
            if (emptyLootLogBudget > 0) {
                emptyLootLogBudget--;
                LOGGER.warn(
                        "Farm spawner loot empty for {} at {} (lootTable={})",
                        mob.getType().getDescriptionId(),
                        spawnerPos,
                        mob.getLootTable()
                );
            }
            return;
        }

        for (ItemStack stack : loot) {
            if (stack.isEmpty()) {
                continue;
            }
            // Remaining stack is discarded when adjacent storage is full / filtered.
            depositIntoAdjacentStorages(level, spawnerPos, stack);
        }
    }

    private static void prepareMobForLoot(Mob mob, Player killer) {
        LivingEntityHurtByPlayerAccessor hurtAccess = (LivingEntityHurtByPlayerAccessor) mob;
        hurtAccess.cointcore$setLastHurtByPlayer(killer);
        hurtAccess.cointcore$setLastHurtByPlayerTime(Math.max(1, mob.tickCount));
        if (mob.isBaby()) {
            // Apothic youthful makes shouldDropLoot() false; loot-table roll does not use that,
            // but keep adult for any mod hooks that still check isBaby().
            mob.setBaby(false);
        }
    }

    private static List<ItemStack> rollDeathLoot(ServerLevel level, Player killer, Mob mob) {
        ResourceKey<LootTable> tableKey = mob.getLootTable();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(tableKey);
        if (table == LootTable.EMPTY) {
            return List.of();
        }

        DamageSource damageSource = level.damageSources().playerAttack(killer);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .withLuck(killer.getLuck())
                .create(LootContextParamSets.ENTITY);

        List<ItemStack> rolled = table.getRandomItems(params);
        List<ItemStack> copy = new ArrayList<>(rolled.size());
        for (ItemStack stack : rolled) {
            if (!stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }
        return copy;
    }

    private static FakePlayer createFakeKiller(ServerLevel level, BlockPos spawnerPos) {
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        fakePlayer.setPos(
                spawnerPos.getX() + 0.5D,
                spawnerPos.getY(),
                spawnerPos.getZ() + 0.5D
        );
        return fakePlayer;
    }

    private static boolean hasFreeSpace(IItemHandler handler) {
        int slotCount = handler.getSlots();
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack depositIntoAdjacentStorages(ServerLevel level, BlockPos spawnerPos, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        for (Direction direction : Direction.values()) {
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }

            IItemHandler handler = SpawnerAdjacentCaps.itemHandler(level, spawnerPos, direction);
            if (handler == null) {
                continue;
            }

            remaining = insertIntoHandler(handler, remaining);
        }
        return remaining;
    }

    private static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack;
        int slotCount = handler.getSlots();
        for (int slot = 0; slot < slotCount; slot++) {
            remaining = handler.insertItem(slot, remaining, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}
