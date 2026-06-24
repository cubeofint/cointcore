package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.mixin.accessor.LivingEntityInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SpawnerLootService {
    private static final double PLAYER_KILL_RANGE = 16.0D;

    private SpawnerLootService() {
    }

    public static boolean hasAdjacentStorageWithSpace(ServerLevel level, BlockPos spawnerPos) {
        for (Direction direction : Direction.values()) {
            IItemHandler handler = getAdjacentHandler(level, spawnerPos, direction);
            if (handler != null && hasFreeSpace(handler)) {
                return true;
            }
        }
        return false;
    }

    public static void generateAndInsert(ServerLevel level, BlockPos spawnerPos, Mob mob) {
        Player killer = resolveLootKiller(level, spawnerPos);
        if (killer == null) {
            return;
        }

        List<ItemStack> loot = simulatePlayerKillLoot(level, killer, mob);
        if (loot.isEmpty() || !canDepositAll(level, spawnerPos, loot)) {
            return;
        }

        for (ItemStack stack : loot) {
            depositIntoAdjacentStorages(level, spawnerPos, stack);
        }
    }

    private static List<ItemStack> simulatePlayerKillLoot(ServerLevel level, Player killer, Mob mob) {
        mob.setLastHurtByPlayer(killer);
        DamageSource damageSource = level.damageSources().playerAttack(killer);

        SpawnerLootCapture.begin();
        try {
            ((LivingEntityInvoker) mob).cointcore$invokeDropAllDeathLoot(level, damageSource);
            return new ArrayList<>(SpawnerLootCapture.end());
        } catch (RuntimeException exception) {
            SpawnerLootCapture.end();
            throw exception;
        }
    }

    private static Player resolveLootKiller(ServerLevel level, BlockPos spawnerPos) {
        if (!shouldGenerateLoot(level, spawnerPos)) {
            return null;
        }

        return createFakeKiller(level, spawnerPos);
    }

    private static boolean shouldGenerateLoot(ServerLevel level, BlockPos spawnerPos) {
        return ApothicSpawnerIntegration.ignoresPlayers(level, spawnerPos)
                || findNearestPlayer(level, spawnerPos, PLAYER_KILL_RANGE) != null;
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

    private static ServerPlayer findNearestPlayer(ServerLevel level, BlockPos spawnerPos, double range) {
        ServerPlayer nearest = null;
        double nearestDistanceSq = range * range;
        double centerX = spawnerPos.getX() + 0.5D;
        double centerY = spawnerPos.getY() + 0.5D;
        double centerZ = spawnerPos.getZ() + 0.5D;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            double distanceSq = player.distanceToSqr(centerX, centerY, centerZ);
            if (distanceSq <= nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = player;
            }
        }

        return nearest;
    }

    private static boolean canDepositAll(ServerLevel level, BlockPos spawnerPos, List<ItemStack> loot) {
        Map<Direction, ItemStack[]> simulated = snapshotAdjacentStorages(level, spawnerPos);
        if (simulated.isEmpty()) {
            return false;
        }

        for (ItemStack stack : loot) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remaining = stack.copy();
            for (Direction direction : Direction.values()) {
                ItemStack[] slots = simulated.get(direction);
                if (slots == null) {
                    continue;
                }

                remaining = simulateInsert(slots, remaining);
                if (remaining.isEmpty()) {
                    break;
                }
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static Map<Direction, ItemStack[]> snapshotAdjacentStorages(ServerLevel level, BlockPos spawnerPos) {
        Map<Direction, ItemStack[]> simulated = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            IItemHandler handler = getAdjacentHandler(level, spawnerPos, direction);
            if (handler != null) {
                simulated.put(direction, copyHandlerStacks(handler));
            }
        }
        return simulated;
    }

    private static ItemStack[] copyHandlerStacks(IItemHandler handler) {
        int slotCount = handler.getSlots();
        ItemStack[] stacks = new ItemStack[slotCount];
        for (int slot = 0; slot < slotCount; slot++) {
            stacks[slot] = handler.getStackInSlot(slot).copy();
        }
        return stacks;
    }

    private static ItemStack simulateInsert(ItemStack[] slots, ItemStack insert) {
        ItemStack remaining = insert.copy();

        for (ItemStack slot : slots) {
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, remaining)) {
                continue;
            }

            int room = slot.getMaxStackSize() - slot.getCount();
            if (room <= 0) {
                continue;
            }

            int move = Math.min(room, remaining.getCount());
            slot.grow(move);
            remaining.shrink(move);
        }

        for (int i = 0; i < slots.length; i++) {
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (slots[i].isEmpty()) {
                slots[i] = remaining.copy();
                return ItemStack.EMPTY;
            }
        }

        return remaining;
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

    private static void depositIntoAdjacentStorages(ServerLevel level, BlockPos spawnerPos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        for (Direction direction : Direction.values()) {
            if (remaining.isEmpty()) {
                return;
            }

            IItemHandler handler = getAdjacentHandler(level, spawnerPos, direction);
            if (handler == null) {
                continue;
            }

            remaining = insertIntoHandler(handler, remaining);
        }
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

    private static IItemHandler getAdjacentHandler(ServerLevel level, BlockPos spawnerPos, Direction direction) {
        BlockPos adjacentPos = spawnerPos.relative(direction);
        BlockEntity blockEntity = level.getBlockEntity(adjacentPos);
        if (blockEntity == null) {
            return null;
        }

        return level.getCapability(Capabilities.ItemHandler.BLOCK, adjacentPos, direction.getOpposite());
    }
}
