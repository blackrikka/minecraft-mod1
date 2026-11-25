package com.blackrikka.ironworks.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * FE buffer shared by Ironworks energy blocks.
 * External {@link #receiveEnergy}/{@link #extractEnergy} calls notify {@code onChanged} when they move energy;
 * {@link #setEnergy} is for internal bookkeeping (generation, NBT load) and does not notify.
 */
public class ModEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (!simulate && received > 0) {
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (!simulate && extracted > 0) {
            onChanged.run();
        }
        return extracted;
    }

    /** Internal update; clamps to [0, capacity]. Does not call {@code onChanged}. */
    public void setEnergy(int value) {
        this.energy = Math.max(0, Math.min(this.capacity, value));
    }
}
