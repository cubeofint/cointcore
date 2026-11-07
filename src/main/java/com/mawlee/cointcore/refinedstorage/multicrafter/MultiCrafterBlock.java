package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.mawlee.cointcore.config.MultiCrafterConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One of the four multiblock crafter blocks. Every part carries the RS network like a cable. */
public final class MultiCrafterBlock extends BaseEntityBlock {
    private final MultiCrafterShape.Part part;
    private final MapCodec<MultiCrafterBlock> codec;

    public MultiCrafterBlock(MultiCrafterShape.Part part) {
        super(Properties.of().strength(1.9F).sound(SoundType.METAL));
        this.part = part;
        this.codec = simpleCodec(properties -> new MultiCrafterBlock(part));
    }

    public MultiCrafterShape.Part part() {
        return part;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return part == MultiCrafterShape.Part.STORAGE
                ? new MultiCrafterStorageBlockEntity(pos, state)
                : new MultiCrafterPartBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (part == MultiCrafterShape.Part.STORAGE) {
            return createTickerHelper(type, MultiCrafters.STORAGE_BLOCK_ENTITY.get(),
                    (tickLevel, pos, tickState, blockEntity) -> blockEntity.serverTick());
        }
        return createTickerHelper(type, MultiCrafters.PART_BLOCK_ENTITY.get(), (tickLevel, pos, tickState, blockEntity) -> {
            blockEntity.updateActiveness(tickState, null);
            blockEntity.doWork();
        });
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) MultiCrafterStructures.invalidate(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MultiCrafterStorageBlockEntity storage) {
                Containers.dropContents(level, pos, storage.getPatternContainer());
            }
            if (!level.isClientSide) MultiCrafterStructures.invalidate(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                                  Player player, net.minecraft.world.InteractionHand hand,
                                                                  BlockHitResult hit) {
        if (part != MultiCrafterShape.Part.STORAGE || stack.isEmpty()
                || !(level.getBlockEntity(pos) instanceof MultiCrafterStorageBlockEntity storage)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        var patterns = storage.getPatternContainer();
        for (int slot = 0; slot < patterns.getContainerSize(); slot++) {
            if (patterns.getItem(slot).isEmpty() && patterns.canPlaceItem(slot, stack)) {
                if (!level.isClientSide) {
                    patterns.setItem(slot, stack.split(1));
                }
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            MultiCrafterShape.Result result = MultiCrafterStructures.get(level, pos);
            if (player.isShiftKeyDown() || !result.valid()) {
                player.displayClientMessage(status(level, pos), false);
            } else {
                Component title = Component.translatable("gui.cointcore.multicrafter.title",
                        result.sizeX() + "×" + result.sizeY() + "×" + result.sizeZ(), result.cpus(),
                        MultiCrafterStorageBlockEntity.stepsFor(result));
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, opener) -> new MultiCrafterMenu(id, inventory, pos, result), title),
                        buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static Component status(Level level, BlockPos pos) {
        MultiCrafterShape.Result result = MultiCrafterStructures.get(level, pos);
        if (!result.valid()) {
            return Component.translatable("message.cointcore.multicrafter.invalid",
                    Component.translatable("message.cointcore.multicrafter.error." + result.error(),
                            result.errorX() + " " + result.errorY() + " " + result.errorZ(),
                            MultiCrafterConfig.getMaxSize())).withStyle(ChatFormatting.RED);
        }
        return Component.translatable("message.cointcore.multicrafter.valid",
                result.sizeX() + "×" + result.sizeY() + "×" + result.sizeZ(),
                result.cpus(), result.storages(),
                result.storages() * MultiCrafterStorageBlockEntity.PATTERNS,
                MultiCrafterStorageBlockEntity.stepsFor(result)).withStyle(ChatFormatting.GREEN);
    }

    /** CPUs past this count add nothing: steps are capped at max_steps_per_tick (1024 / 4 = 256 by default). */
    static int usefulCpus() {
        int perCpu = Math.max(1, MultiCrafterConfig.getStepsPerCpu());
        return (MultiCrafterConfig.getMaxStepsPerTick() + perCpu - 1) / perCpu;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cointcore.multicrafter." + part.name().toLowerCase(java.util.Locale.ROOT),
                MultiCrafterConfig.getStepsPerCpu(), MultiCrafterStorageBlockEntity.PATTERNS,
                usefulCpus(), MultiCrafterConfig.getMaxStepsPerTick()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cointcore.multicrafter.layout").withStyle(ChatFormatting.DARK_GRAY));
    }
}
