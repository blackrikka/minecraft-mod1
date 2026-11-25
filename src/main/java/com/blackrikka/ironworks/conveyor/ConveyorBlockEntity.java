package com.blackrikka.ironworks.conveyor;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Conveyor buffer: always hands the item to the block in front. */
public class ConveyorBlockEntity extends AbstractBeltBlockEntity {
    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR.get(), pos, state);
    }

    @Override
    protected boolean deliver(Level level, BlockPos pos, Direction facing, ItemStack stack) {
        return offer(level, pos, facing, stack);
    }
}
