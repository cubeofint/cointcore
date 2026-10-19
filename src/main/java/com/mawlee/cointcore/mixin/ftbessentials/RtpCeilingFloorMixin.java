package com.mawlee.cointcore.mixin.ftbessentials;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.ftbessentials.RtpFloorSearch;
import dev.ftb.mods.ftbessentials.commands.groups.TeleportingCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FTB Essentials {@code /rtp} stands the player on the heightmap. Where the column is topped by bedrock (Nether
 * roof, mining dimensions) that is the roof, so players land on or in bedrock. Swap the heightmap result for
 * the first safe floor under the roof; with none, return the build floor so FTB skips the attempt
 * ({@code hmPos.getY() > 0}) and tries another spot.
 */
@Mixin(value = TeleportingCommands.class, remap = false)
public abstract class RtpCeilingFloorMixin {
    @WrapOperation(
            method = "findBlockPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"
            ),
            require = 0
    )
    private static BlockPos cointcore$floorUnderBedrockRoof(
            ServerLevel level,
            Heightmap.Types type,
            BlockPos pos,
            Operation<BlockPos> original
    ) {
        BlockPos top = original.call(level, type, pos);
        if (!level.getBlockState(top.below()).is(Blocks.BEDROCK)) {
            return top;
        }
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int x = top.getX();
        int z = top.getZ();
        int standY = RtpFloorSearch.findStandY(
                top.getY(),
                level.getMinBuildHeight(),
                y -> cointcore$isFree(level.getBlockState(probe.set(x, y, z))),
                y -> cointcore$isFloor(level.getBlockState(probe.set(x, y, z)))
        );
        return new BlockPos(x, standY == RtpFloorSearch.NOT_FOUND ? level.getMinBuildHeight() : standY, z);
    }

    @Unique
    private static boolean cointcore$isFree(BlockState state) {
        return !state.blocksMotion() && state.getFluidState().isEmpty();
    }

    @Unique
    private static boolean cointcore$isFloor(BlockState state) {
        return state.blocksMotion() && !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty();
    }
}
