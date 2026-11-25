package com.blackrikka.ironworks.conveyor;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Splitter buffer: round-robin over front, left and right. */
public class SplitterBlockEntity extends AbstractBeltBlockEntity {
    public SplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPLITTER.get(), pos, state);
    }

    @Override
    protected boolean deliver(Level level, BlockPos pos, Direction facing, ItemStack stack) {
        Direction[] outputs = {facing, facing.getCounterClockWise(), facing.getClockWise()};
        for (int i = 0; i < outputs.length; i++) {
            int index = (roundRobin + i) % outputs.length;
            if (offer(level, pos, outputs[index], stack)) {
                roundRobin = (index + 1) % outputs.length;
                return true;
            }
        }
        return false;
    }
}
