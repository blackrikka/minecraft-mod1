package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Holds the running total of FE received by a {@link CreativeBatteryBlock}. */
public class CreativeBatteryBlockEntity extends BlockEntity {
    private long totalReceived;

    /** Receive-only sink: accepts every offer in full and keeps a running total. */
    private final IEnergyStorage sink = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) {
                return 0;
            }
            if (!simulate) {
                totalReceived += toReceive;
                setChanged();
            }
            return toReceive;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public CreativeBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_BATTERY.get(), pos, state);
    }

    public IEnergyStorage getEnergySink() {
        return sink;
    }

    public long getTotalReceived() {
        return totalReceived;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("totalReceived", totalReceived);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        totalReceived = tag.getLong("totalReceived");
    }
}
