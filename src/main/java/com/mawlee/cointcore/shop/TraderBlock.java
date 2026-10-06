package com.mawlee.cointcore.shop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Trade terminal. The model front (keypad and card slot) faces north at rotation 0.
 */
public class TraderBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TraderBlock> CODEC = simpleCodec(TraderBlock::new);

    /** North-facing boxes as {minX, minY, minZ, maxX, maxY, maxZ} in model pixels. */
    private static final double[][] NORTH_BOXES = {
            {0, 0, 0, 16, 5, 16},
            {0, 5, 7, 16, 16, 16},
            {0, 13.5, 3, 16, 16, 7}
    };

    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public TraderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, opener) -> new TraderMenu(
                            containerId,
                            inventory,
                            ContainerLevelAccess.create(level, pos)
                    ),
                    Component.translatable("container.cointcore.trader")
            ));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : NORTH_BOXES) {
                shape = Shapes.or(shape, rotated(box, facing));
            }
            shapes.put(facing, shape.optimize());
        }
        return shapes;
    }

    /** Rotates a north-facing box clockwise (seen from above) to match blockstate y-rotation. */
    private static VoxelShape rotated(double[] box, Direction facing) {
        double minX = box[0];
        double minZ = box[2];
        double maxX = box[3];
        double maxZ = box[5];
        return switch (facing) {
            case SOUTH -> Block.box(16 - maxX, box[1], 16 - maxZ, 16 - minX, box[4], 16 - minZ);
            case EAST -> Block.box(16 - maxZ, box[1], minX, 16 - minZ, box[4], maxX);
            case WEST -> Block.box(minZ, box[1], 16 - maxX, maxZ, box[4], 16 - minX);
            default -> Block.box(minX, box[1], minZ, maxX, box[4], maxZ);
        };
    }
}
