package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.storage.terminal.DbTerminalBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Network cable (data only, no energy). Same six-face connection scheme as the copper cable: a face is connected
 * when the neighbour is another network cable, a server rack or a storage port. Multipart model, one hitbox per mask.
 */
public class NetworkCableBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN);

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARM_BY_DIRECTION = Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6),
            Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16),
            Direction.WEST, Block.box(0, 6, 6, 6, 10, 10),
            Direction.EAST, Block.box(10, 6, 6, 16, 10, 10),
            Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10),
            Direction.UP, Block.box(6, 10, 6, 10, 16, 10));

    /** Shape for each connection mask (bit = {@link Direction#ordinal()}), built once. */
    private static final VoxelShape[] SHAPES = buildShapes();

    public NetworkCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<NetworkCableBlock> codec() {
        return simpleCodec(NetworkCableBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** True if the cable's face {@code direction} is connected. */
    public static boolean isConnected(BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    /** True for the blocks a network cable connects to: cables, server racks, storage ports and DB terminals. */
    public static boolean isNetworkNode(BlockState state) {
        return state.getBlock() instanceof NetworkCableBlock
                || state.getBlock() instanceof ServerRackBlock
                || state.getBlock() instanceof StoragePortBlock
                || state.getBlock() instanceof DbTerminalBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        refreshConnections(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos fromPos, boolean movedByPiston) {
        refreshConnections(state, level, pos);
    }

    /** Cheap update when a neighbour changes shape: nodes connect, air disconnects, anything else is left alone. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isNetworkNode(neighborState)) {
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), true);
        }
        if (neighborState.isAir()) {
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPES[connectionMask(state)];
    }

    /** Recomputes all six connection flags from the world. */
    public static BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            result = result.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    isNetworkNode(level.getBlockState(pos.relative(direction))));
        }
        return result;
    }

    private static void refreshConnections(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState updated = computeConnections(state, level, pos);
        if (!updated.equals(state)) {
            // Only changes are written, so the update triggered by setBlock stops after one round.
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (isConnected(state, direction)) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) {
                if ((mask & (1 << direction.ordinal())) != 0) {
                    shape = Shapes.or(shape, ARM_BY_DIRECTION.get(direction));
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }
}
