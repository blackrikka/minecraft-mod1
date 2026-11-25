package com.blackrikka.ironworks.block;

import com.blackrikka.ironworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Rubber tree log. {@code sap = true} means the log can be tapped for {@code rubber_sap}.
 * Tapping sets {@code sap = false}; an untapped log regenerates sap on random ticks.
 */
public class RubberLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty SAP = BooleanProperty.create("sap");

    /** Each random tick on an untapped log has a 1 in this chance to regenerate sap. */
    private static final int SAP_REGEN_ONE_IN = 10;

    public RubberLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(SAP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SAP);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(SAP)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(SAP, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        ItemStack sap = new ItemStack(ModItems.RUBBER_SAP.get());
        if (!player.getInventory().add(sap)) {
            popResource(level, pos, sap);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(SAP);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SAP) && random.nextInt(SAP_REGEN_ONE_IN) == 0) {
            level.setBlock(pos, state.setValue(SAP, true), Block.UPDATE_CLIENTS);
        }
    }
}
