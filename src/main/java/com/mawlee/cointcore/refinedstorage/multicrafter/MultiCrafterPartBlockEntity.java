package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.mawlee.cointcore.config.MultiCrafterConfig;
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Frame, vent and CPU blocks: plain network nodes that pass the RS network through, so the crafter can be connected
 * from any side. CPUs draw energy; the speed itself is applied by the pattern storages.
 */
public final class MultiCrafterPartBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<SimpleNetworkNode> {
    public MultiCrafterPartBlockEntity(BlockPos pos, BlockState state) {
        super(MultiCrafters.PART_BLOCK_ENTITY.get(), pos, state, new SimpleNetworkNode(energyFor(state)));
    }

    private static long energyFor(BlockState state) {
        return state.getBlock() instanceof MultiCrafterBlock block && block.part() == MultiCrafterShape.Part.CPU
                ? MultiCrafterConfig.getCpuEnergy() : 0;
    }

    @Override
    protected boolean hasRedstoneMode() {
        return false;
    }

    @Override
    public Component getName() {
        return overrideName(getBlockState().getBlock().getName());
    }
}
