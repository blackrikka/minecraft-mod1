package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Port buffer-less item handler: every insert goes straight to the network. The network search is cached for
 * {@link #NETWORK_CACHE_TICKS} game ticks, so a hopper pushing every few ticks does not search each time.
 */
public class StoragePortBlockEntity extends BlockEntity {
    public static final long NETWORK_CACHE_TICKS = 20;

    private final IItemHandler itemHandler = new PortItemHandler();

    private StorageNetwork network;
    private long networkTick;

    public StoragePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_PORT.get(), pos, state);
    }

    /** Item handler for pipes: one slot, insert only. */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** The network this port belongs to, refreshed at most every {@link #NETWORK_CACHE_TICKS} ticks. */
    public StorageNetwork getNetwork() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return StorageNetwork.EMPTY;
        }
        long now = level.getGameTime();
        if (network == null || now < networkTick || now - networkTick >= NETWORK_CACHE_TICKS) {
            network = StorageNetwork.collect(level, worldPosition);
            networkTick = now;
        }
        return network;
    }

    private final class PortItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            long accepted = getNetwork().insert(stack, stack.getCount(), simulate);
            return stack.copyWithCount(stack.getCount() - (int) accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Items only go into the network through a port; pipes cannot pull them out.
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
