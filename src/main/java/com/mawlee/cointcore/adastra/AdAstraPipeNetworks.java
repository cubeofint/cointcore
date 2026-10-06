package com.mawlee.cointcore.adastra;

import earth.terrarium.adastra.common.blockentities.pipes.PipeBlockEntity;
import earth.terrarium.adastra.common.blocks.pipes.PipeBlock;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Pipe controllers keep their last source and consumer maps until a connected
 * pipe changes. The controller itself is marked, not only the pipe that was
 * updated.
 */
public final class AdAstraPipeNetworks {
    private static final Map<PipeBlockEntity, Boolean> DIRTY = Collections.synchronizedMap(new WeakHashMap<>());

    private AdAstraPipeNetworks() {
    }

    public static void markDirty(PipeBlockEntity pipe) {
        DIRTY.put(pipe, Boolean.TRUE);
    }

    /**
     * A machine placed against one pipe has to dirty the controller, which may
     * sit at the other end of the line. Walk the connected pipes once.
     */
    public static void markNetworkDirty(Level level, BlockPos origin) {
        if (level.getBlockEntity(origin) instanceof PipeBlockEntity originPipe) {
            markDirty(originPipe);
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        queue.add(origin);
        seen.add(origin.asLong());
        int walked = 0;
        while (!queue.isEmpty() && walked < 4096) {
            BlockPos pos = queue.removeFirst();
            walked++;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PipeBlock)) {
                continue;
            }
            for (Direction direction : PipeBlock.getConnectedDirections(state)) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next.asLong())) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof PipeBlockEntity pipe) {
                    markDirty(pipe);
                    queue.add(next);
                }
            }
        }
    }

    public static boolean needsRebuild(PipeBlockEntity pipe) {
        return DIRTY.getOrDefault(pipe, Boolean.TRUE);
    }

    public static void markClean(PipeBlockEntity pipe) {
        DIRTY.put(pipe, Boolean.FALSE);
    }
}
