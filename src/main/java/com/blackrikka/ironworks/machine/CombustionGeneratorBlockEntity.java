package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.energy.ModEnergyStorage;
import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Server-side logic of the combustion generator.
 * Fuel is taken from a single slot; while burning it produces {@link #PRODUCTION_PER_TICK} FE per tick
 * into an internal buffer. If the buffer has no room for one tick of output, the generator stops and does
 * not consume fuel. The buffer is pushed to all six neighbours through {@code Capabilities.EnergyStorage.BLOCK}.
 */
public class CombustionGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 10_000;
    public static final int PRODUCTION_PER_TICK = 10;
    /** Maximum FE pushed out of this block per face per tick. */
    public static final int MAX_PUSH_PER_FACE_PER_TICK = 40;

    private static final int FUEL_SLOT = 0;
    private static final int DATA_COUNT = 4;

    private final FuelItemHandler inventory = new FuelItemHandler(this::setChanged);
    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, MAX_PUSH_PER_FACE_PER_TICK, this::setChanged);
    private final FuelInsertOnlyHandler fuelHandler = new FuelInsertOnlyHandler(inventory);

    /** Ticks of fuel left in the current item. */
    private int burnTime;
    /** Burn time of the item that is currently burning (for the GUI flame gauge). */
    private int burnTotal;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> burnTime;
                case 3 -> burnTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Values are only written by the server; the client never changes them.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state);
    }

    /** True if the item can be burned as fuel (vanilla fuel, mod fuel included). */
    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean generating = false;
        boolean hasRoom = energy.getEnergyStored() + PRODUCTION_PER_TICK <= energy.getMaxEnergyStored();

        if (hasRoom) {
            if (burnTime <= 0) {
                startBurningFuel(level, pos);
            }
            if (burnTime > 0) {
                burnTime--;
                energy.setEnergy(energy.getEnergyStored() + PRODUCTION_PER_TICK);
                generating = true;
                setChanged();
            }
        }

        pushEnergy(level, pos);

        if (state.getValue(CombustionGeneratorBlock.LIT) != generating) {
            // setBlock with UPDATE_ALL also sends the block update to clients.
            level.setBlock(pos, state.setValue(CombustionGeneratorBlock.LIT, generating), Block.UPDATE_ALL);
        }
    }

    /** Consumes one fuel item and starts its burn timer. Returns without change if slot holds no fuel. */
    private void startBurningFuel(Level level, BlockPos pos) {
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        int time = fuel.getBurnTime(RecipeType.SMELTING);
        if (time <= 0) {
            return;
        }
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            inventory.setStackInSlot(FUEL_SLOT, remainder);
        } else if (!remainder.isEmpty()) {
            // Stack still holds items, so the container item cannot go back into the same slot.
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, remainder);
        }
        burnTime = time;
        burnTotal = time;
        setChanged();
    }

    /** Pushes up to {@link #MAX_PUSH_PER_FACE_PER_TICK} FE per face into neighbours that accept energy. */
    private void pushEnergy(Level level, BlockPos pos) {
        if (energy.getEnergyStored() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null) {
                continue;
            }
            int offer = energy.extractEnergy(MAX_PUSH_PER_FACE_PER_TICK, true);
            if (offer <= 0) {
                break;
            }
            int accepted = target.receiveEnergy(offer, false);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
            }
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
    }

    public ModEnergyStorage getEnergyStorage() {
        return energy;
    }

    public IItemHandler getFuelHandler() {
        return fuelHandler;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("burnTime", burnTime);
        tag.putInt("burnTotal", burnTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        energy.setEnergy(tag.getInt("energy"));
        burnTime = tag.getInt("burnTime");
        burnTotal = tag.getInt("burnTotal");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ironworks.combustion_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, inventory, dataAccess,
                ContainerLevelAccess.create(level, worldPosition));
    }

    /** Single fuel slot. Only fuel is accepted; changes mark the block entity dirty. */
    public static final class FuelItemHandler extends ItemStackHandler {
        private final Runnable onChanged;

        public FuelItemHandler(Runnable onChanged) {
            super(1);
            this.onChanged = onChanged;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onChanged.run();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }
    }

    /** Exposed to hoppers and conveyors: accepts fuel only, never gives items back. */
    public static final class FuelInsertOnlyHandler implements IItemHandler {
        private final ItemStackHandler delegate;

        public FuelInsertOnlyHandler(ItemStackHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot).copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isFuel(stack)) {
                return stack;
            }
            return delegate.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }
    }
}
