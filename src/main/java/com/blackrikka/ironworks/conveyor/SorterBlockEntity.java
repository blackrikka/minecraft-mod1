package com.blackrikka.ironworks.conveyor;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sorter buffer. The filter is one item type (stored as a single-count stack). An item that matches goes straight
 * ahead and waits if the front is blocked; a non-matching item (or every item while no filter is set) goes left or
 * right in turn.
 */
public class SorterBlockEntity extends AbstractBeltBlockEntity {
    private ItemStack filter = ItemStack.EMPTY;

    public SorterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTER.get(), pos, state);
    }

    /** The filter item (empty when no filter is set). Do not modify the returned stack. */
    public ItemStack getFilter() {
        return filter;
    }

    /** Sets the filter to one item of {@code stack}'s type; an empty stack clears it. */
    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected boolean deliver(Level level, BlockPos pos, Direction facing, ItemStack stack) {
        if (!filter.isEmpty() && ItemStack.isSameItem(stack, filter)) {
            return offer(level, pos, facing, stack);
        }
        Direction[] sides = {facing.getCounterClockWise(), facing.getClockWise()};
        for (int i = 0; i < sides.length; i++) {
            int index = (roundRobin + i) % sides.length;
            if (offer(level, pos, sides[index], stack)) {
                roundRobin = (index + 1) % sides.length;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!filter.isEmpty()) {
            tag.put("filter", filter.saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filter = ItemStack.parseOptional(registries, tag.getCompound("filter"));
    }
}
