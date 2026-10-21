package com.mawlee.cointcore.ae;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IStorageProvider;
import appeng.parts.AEBasePart;
import com.mawlee.cointcore.chunklimit.PartLimitIndex;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * AE2 side of the cable-bus part limits ({@link PartLimitIndex}): lists limited parts in a chunk and remounts storage
 * buses whose active state changed. Only loaded when AE2 is present.
 */
public final class Ae2PartLimits implements PartLimitIndex.Scanner {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int CENTER = DIRECTIONS.length;

    private Ae2PartLimits() {
    }

    public static void install() {
        PartLimitIndex.setScanner(new Ae2PartLimits());
    }

    public static ResourceLocation itemId(IPart part) {
        return BuiltInRegistries.ITEM.getKey(part.getPartItem().asItem());
    }

    public static int sideIndex(Direction side) {
        return side == null ? CENTER : side.ordinal();
    }

    /** False for a part past its chunk limit (storage buses then stop mounting and scanning). */
    public static boolean isActive(AEBasePart part) {
        if (!ChunkLimitConfig.isEnabled() || !(part.getLevel() instanceof ServerLevel level)) {
            return true;
        }
        BlockEntity host = part.getBlockEntity();
        return host == null || PartLimitIndex.isActive(level, host.getBlockPos(), sideIndex(part.getSide()));
    }

    /** Part added or removed: rescan its chunk next tick when it is a limited kind. */
    public static void onPartChanged(AEBasePart part) {
        if (!(part.getLevel() instanceof ServerLevel level) || !ChunkLimitConfig.isEnabled()) {
            return;
        }
        BlockEntity host = part.getBlockEntity();
        if (host != null && ChunkLimitConfig.isLimitedPart(itemId(part))) {
            PartLimitIndex.markDirty(level, host.getBlockPos());
        }
    }

    @Override
    public List<PartLimitIndex.FoundPart> scan(LevelChunk chunk) {
        List<PartLimitIndex.FoundPart> found = new ArrayList<>();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof IPartHost host)) {
                continue;
            }
            for (int side = 0; side <= CENTER; side++) {
                IPart part = host.getPart(side == CENTER ? null : DIRECTIONS[side]);
                if (part == null) {
                    continue;
                }
                ResourceLocation id = itemId(part);
                if (ChunkLimitConfig.isLimitedPart(id)) {
                    found.add(new PartLimitIndex.FoundPart(blockEntity.getBlockPos(), side, id));
                }
            }
        }
        return found;
    }

    @Override
    public void onActivityChanged(ServerLevel level, BlockPos pos, int side) {
        if (!(level.getBlockEntity(pos) instanceof IPartHost host)) {
            return;
        }
        IPart part = host.getPart(side == CENTER ? null : DIRECTIONS[side]);
        if (part instanceof IStorageProvider && part instanceof AEBasePart base) {
            IStorageProvider.requestUpdate(base.getMainNode());
            base.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
    }
}
