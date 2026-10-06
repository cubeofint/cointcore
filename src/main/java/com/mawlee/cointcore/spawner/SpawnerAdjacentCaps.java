package com.mawlee.cointcore.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Adjacent capability lookup for farm-spawner deposits.
 * Tries the facing side first, then {@code null} — many tanks/chests only expose caps without a side.
 */
public final class SpawnerAdjacentCaps {
    private SpawnerAdjacentCaps() {
    }

    public static IItemHandler itemHandler(ServerLevel level, BlockPos spawnerPos, Direction direction) {
        BlockPos adjacentPos = spawnerPos.relative(direction);
        Direction towardSpawner = direction.getOpposite();
        IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, adjacentPos, towardSpawner);
        if (sided != null) {
            return sided;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, adjacentPos, null);
    }

    public static IFluidHandler fluidHandler(ServerLevel level, BlockPos spawnerPos, Direction direction) {
        BlockPos adjacentPos = spawnerPos.relative(direction);
        Direction towardSpawner = direction.getOpposite();
        IFluidHandler sided = level.getCapability(Capabilities.FluidHandler.BLOCK, adjacentPos, towardSpawner);
        if (sided != null) {
            return sided;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, adjacentPos, null);
    }
}
