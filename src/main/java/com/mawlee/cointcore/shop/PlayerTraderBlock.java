package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Player-owned vending machine. Reuses the system trader model/shape.
 */
public class PlayerTraderBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PlayerTraderBlock> CODEC = simpleCodec(PlayerTraderBlock::new);

    private static final double[][] NORTH_BOXES = {
            {0, 0, 0, 16, 5, 16},
            {0, 5, 7, 16, 16, 16},
            {0, 13.5, 3, 16, 16, 7}
    };

    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public PlayerTraderBlock(Properties properties) {
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
        if (context.getPlayer() instanceof ServerPlayer serverPlayer && !PlayerShopAccess.canPlace(serverPlayer)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop && placer != null) {
            shop.setOwner(placer.getUUID(), placer.getName().getString());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerTraderBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop && !shop.canManage(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop) {
            shop.dropStock(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
            if (!PlayerShopAccess.canUse(serverPlayer)) {
                serverPlayer.sendSystemMessage(
                        CointCoreMessages.forPlayer(serverPlayer, CointCoreMessages.PLAYER_SHOP_NO_USE)
                );
                return InteractionResult.FAIL;
            }
            if (!(level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop)) {
                return InteractionResult.FAIL;
            }
            PlayerTraderMenus.open(serverPlayer, shop, false);
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
