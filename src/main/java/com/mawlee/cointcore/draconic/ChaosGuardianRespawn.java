package com.mawlee.cointcore.draconic;

import com.brandon3055.brandonscore.worldentity.WorldEntity;
import com.brandon3055.brandonscore.worldentity.WorldEntityHandler;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.draconicevolution.entity.guardian.GuardianFightManager;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mawlee.cointcore.config.ChaosGuardianRespawnConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Re-arms every Draconic Evolution chaos island in the End: puts the chaos crystal back if players broke it and
 * registers a {@link GuardianFightManager}, the same way island worldgen does. The guardian then spawns when a player
 * reaches the island. Islands with a fight still in progress, and islands in chunks that were never generated, are
 * left alone. Only touches DE classes, so it must be called only when DE is loaded.
 */
final class ChaosGuardianRespawn {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ChaosGuardianRespawn() {
    }

    static void run(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        int separation = DEConfig.chaosIslandSeparation;
        if (end == null || !DEConfig.chaosIslandEnabled || separation <= 0) {
            return;
        }
        int steps = ChaosGuardianRespawnConfig.getMaxRadiusBlocks() / separation;
        int armed = 0;
        int restored = 0;
        int active = 0;
        int blocked = 0;
        for (int gx = -steps; gx <= steps; gx++) {
            for (int gz = -steps; gz <= steps; gz++) {
                if (gx == 0 && gz == 0) {
                    continue; // DE never places an island at the main End island
                }
                BlockPos pos = new BlockPos(gx * separation, DEConfig.chaosIslandYPosition, gz * separation);
                if (!end.getWorldBorder().isWithinBounds(pos) || !isGenerated(end, new ChunkPos(pos))) {
                    continue;
                }
                switch (arm(end, pos)) {
                    case ARMED -> armed++;
                    case RESTORED -> {
                        armed++;
                        restored++;
                    }
                    case ACTIVE -> active++;
                    case BLOCKED -> blocked++;
                }
            }
        }
        LOGGER.info("Chaos guardians re-armed on {} islands ({} crystals restored), {} fights already active, {} blocked",
                armed, restored, active, blocked);
    }

    private enum Result { ARMED, RESTORED, ACTIVE, BLOCKED }

    private static Result arm(ServerLevel level, BlockPos pos) {
        level.getChunk(pos); // already generated (checked), so this only loads it
        if (hasFight(level, pos)) {
            return Result.ACTIVE;
        }
        boolean restored = false;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TileChaosCrystal)) {
            BlockState current = level.getBlockState(pos);
            if (!current.isAir() && !ChaosGuardianRespawnConfig.isReplaceBlocks()) {
                LOGGER.warn("Chaos island at {} {} {}: {} stands where the chaos crystal goes, not restoring",
                        pos.getX(), pos.getY(), pos.getZ(), current.getBlock());
                return Result.BLOCKED;
            }
            level.setBlock(pos, DEContent.CHAOS_CRYSTAL.get().defaultBlockState(), 3);
            blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof TileChaosCrystal placed)) {
                return Result.BLOCKED;
            }
            placed.onValidPlacement(); // locks it to this spot and builds the upper/lower parts on its first tick
            restored = true;
        }
        TileChaosCrystal crystal = (TileChaosCrystal) blockEntity;
        crystal.guardianDefeated.set(false);
        crystal.setChanged();
        WorldEntityHandler.addWorldEntity(level, new GuardianFightManager(pos));
        return restored ? Result.RESTORED : Result.ARMED;
    }

    private static boolean hasFight(ServerLevel level, BlockPos pos) {
        for (WorldEntity entity : WorldEntityHandler.getWorldEntities()) {
            if (entity instanceof GuardianFightManager manager && !manager.isRemoved()
                    && manager.getLevel() == level && pos.equals(manager.getArenaOrigin())) {
                return true;
            }
        }
        return false;
    }

    /** True for chunks that exist and are fully generated; never triggers generation. */
    private static boolean isGenerated(ServerLevel level, ChunkPos chunk) {
        if (level.hasChunk(chunk.x, chunk.z)) {
            return true;
        }
        Optional<CompoundTag> tag = level.getChunkSource().chunkMap.read(chunk).join();
        return tag.map(nbt -> nbt.getString("Status").endsWith("full")).orElse(false);
    }
}
