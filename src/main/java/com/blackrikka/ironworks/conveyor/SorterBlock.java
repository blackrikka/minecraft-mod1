package com.blackrikka.ironworks.conveyor;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Sorter: items matching the filter go straight ahead; all others go left or right in turn.
 * Right-click with an item sets the filter, sneak + right-click with an empty hand clears it,
 * and a plain right-click with an empty hand shows the current filter on the action bar.
 */
public class SorterBlock extends AbstractBeltBlock {
    public SorterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SorterBlock> codec() {
        return simpleCodec(SorterBlock::new);
    }

    @Override
    protected BlockEntityType<? extends AbstractBeltBlockEntity> beltType() {
        return ModBlockEntities.SORTER.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SorterBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SorterBlockEntity be) {
            be.setFilter(stack);
            player.displayClientMessage(
                    Component.translatable("message.ironworks.sorter.filter", stack.getHoverName()), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SorterBlockEntity be) {
            if (player.isShiftKeyDown()) {
                be.setFilter(ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("message.ironworks.sorter.cleared"), true);
            } else if (be.getFilter().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.ironworks.sorter.filter_none"), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.ironworks.sorter.filter", be.getFilter().getHoverName()), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
