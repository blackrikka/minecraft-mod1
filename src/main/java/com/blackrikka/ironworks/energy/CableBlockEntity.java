package com.blackrikka.ironworks.energy;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Buffer and distributor of one copper cable block.
 * Energy enters through {@link #getExternalEnergy()} (receive-only, {@value #RECEIVE_PER_TICK} FE per game tick
 * in total). Each server tick the buffer is pushed to connected non-cable neighbours (round-robin) and to
 * connected cable neighbours holding less energy (half the difference, at least 1 FE).
 */
public class CableBlockEntity extends BlockEntity {
    public static final int CAPACITY = 512;
    public static final int RECEIVE_PER_TICK = 128;
    public static final int TRANSFER_PER_TICK = 128;

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, RECEIVE_PER_TICK, TRANSFER_PER_TICK, this::setChanged);
    private final IEnergyStorage externalView = new ExternalView();

    /** Game tick that {@link #receivedThisTick} belongs to. */
    private long receiveTick = Long.MIN_VALUE;
    private int receivedThisTick;
    /** Rotates the first face that gets served each tick. */
    private int roundRobin;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_CABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CableBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (energy.getEnergyStored() <= 0) {
            return;
        }

        Direction[] directions = Direction.values();
        int budget = TRANSFER_PER_TICK;

        // Cable neighbours first: only toward a neighbour that holds less energy, so energy does not ping-pong.
        // Balancing runs before the sinks; otherwise a sink that always accepts would drain this buffer
        // and the cables behind it would never receive anything.
        for (Direction direction : directions) {
            if (budget <= 0) {
                break;
            }
            if (!CableBlock.isConnected(state, direction)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            if (!(level.getBlockEntity(neighborPos) instanceof CableBlockEntity other)) {
                continue;
            }
            int mine = energy.getEnergyStored();
            int theirs = other.energy.getEnergyStored();
            if (mine <= theirs) {
                continue;
            }
            int amount = Math.min(budget, Math.max(1, (mine - theirs) / 2));
            budget -= pushTo(level, neighborPos, direction, amount);
        }

        // Non-cable receivers (machines, batteries, other mods) on connected faces, round-robin start.
        int start = roundRobin;
        roundRobin = (roundRobin + 1) % directions.length;
        for (int i = 0; i < directions.length && budget > 0; i++) {
            Direction direction = directions[(start + i) % directions.length];
            if (!CableBlock.isConnected(state, direction)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockState(neighborPos).getBlock() instanceof CableBlock) {
                continue;
            }
            budget -= pushTo(level, neighborPos, direction, budget);
        }
    }

    /** Offers up to {@code maxAmount} FE to the capability of {@code targetPos}. Returns the FE accepted. */
    private int pushTo(Level level, BlockPos targetPos, Direction direction, int maxAmount) {
        if (maxAmount <= 0) {
            return 0;
        }
        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
        if (target == null) {
            return 0;
        }
        int offer = energy.extractEnergy(maxAmount, true);
        if (offer <= 0) {
            return 0;
        }
        int accepted = target.receiveEnergy(offer, false);
        if (accepted > 0) {
            energy.extractEnergy(accepted, false);
        }
        return Math.max(accepted, 0);
    }

    public IEnergyStorage getExternalEnergy() {
        return externalView;
    }

    public int getStoredEnergy() {
        return energy.getEnergyStored();
    }

    public int getCapacity() {
        return energy.getMaxEnergyStored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("energy"));
    }

    /** Receive-only view handed to neighbours. Extraction is internal, so external extraction always returns 0. */
    private final class ExternalView implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            Level level = getLevel();
            if (toReceive <= 0 || level == null) {
                return 0;
            }
            long now = level.getGameTime();
            if (now != receiveTick) {
                receiveTick = now;
                receivedThisTick = 0;
            }
            int allowed = Math.min(toReceive, RECEIVE_PER_TICK - receivedThisTick);
            if (allowed <= 0) {
                return 0;
            }
            int accepted = energy.receiveEnergy(allowed, simulate);
            if (!simulate) {
                receivedThisTick += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
