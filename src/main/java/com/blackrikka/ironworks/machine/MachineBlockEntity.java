package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.energy.ModEnergyStorage;
import com.blackrikka.ironworks.recipe.MachineRecipe;
import com.blackrikka.ironworks.recipe.MachineRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Base block entity of the processing machines.
 * <p>
 * Inventory layout: slots {@code [0, inputSlots)} are inputs, the last slot is the output.
 * Each server tick: find a craft for the current inputs, check that the output has room and the buffer holds
 * {@link Craft#energyPerTick()} FE, then advance progress by one. Progress stops (without decreasing) when power
 * is short. When progress reaches {@link Craft#time()}, the inputs are consumed and the result is inserted.
 * <p>
 * External access: hoppers and conveyors insert into input slots and extract from the output slot only
 * ({@link #getItemView()}); energy is receive-only ({@link #getEnergyStorage()}).
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 10_000;
    public static final int MAX_RECEIVE_PER_TICK = 128;
    /** Number of values exposed through {@link #getDataAccess()}: energy, max energy, progress, max progress. */
    public static final int DATA_COUNT = 4;

    /** A craft that can be performed on the current inputs. Slot and count arrays are parallel. */
    protected record Craft(ItemStack result, int time, int energyPerTick, int[] slots, int[] counts) {
    }

    private final int inputSlots;
    private final ItemStackHandler inventory;
    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, MAX_RECEIVE_PER_TICK, 0, this::setChanged);
    private final IItemHandler itemView = new ItemView();

    /** Ticks of work done on the current craft. */
    private int progress;
    /** Ticks the current craft needs (0 when idle). */
    private int maxProgress;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> progress;
                case 3 -> maxProgress;
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

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputSlots) {
        super(type, pos, state);
        this.inputSlots = inputSlots;
        this.inventory = new ItemStackHandler(inputSlots + 1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < inputSlots && isValidInput(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    /** True if {@code stack} can be placed into an input slot (checked against the machine's recipes). */
    protected abstract boolean isValidInput(ItemStack stack);

    /** The craft that the current inputs allow, or {@code null}. */
    @Nullable
    protected abstract Craft findCraft(Level level);

    /** Menu type used when a player opens this machine. */
    protected abstract MenuType<?> menuType();

    /** Shared lookup for {@link MachineRecipe} types: returns the craft, or {@code null} if nothing matches. */
    @Nullable
    protected final Craft findMachineCraft(Level level, RecipeType<MachineRecipe> type) {
        MachineRecipeInput input = MachineRecipeInput.of(inventory, inputSlots);
        Optional<RecipeHolder<MachineRecipe>> holder = level.getRecipeManager().getRecipeFor(type, input, level);
        if (holder.isEmpty()) {
            return null;
        }
        MachineRecipe recipe = holder.get().value();
        int[] slots = recipe.findSlots(input);
        if (slots == null) {
            return null;
        }
        int[] counts = recipe.getEntries().stream().mapToInt(MachineRecipe.Entry::count).toArray();
        return new Craft(recipe.getResult(), recipe.getTime(), recipe.getEnergyPerTick(), slots, counts);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        Craft craft = findCraft(level);
        boolean working = false;

        if (craft == null) {
            progress = 0;
            maxProgress = 0;
        } else {
            maxProgress = craft.time();
            if (canInsertOutput(craft.result()) && energy.getEnergyStored() >= craft.energyPerTick()) {
                energy.setEnergy(energy.getEnergyStored() - craft.energyPerTick());
                progress++;
                working = true;
                if (progress >= maxProgress) {
                    finishCraft(craft);
                    progress = 0;
                }
                setChanged();
            }
        }

        if (state.getValue(MachineBlock.LIT) != working) {
            // setBlock with UPDATE_ALL also sends the block update to clients.
            level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
        }
    }

    private int outputSlot() {
        return inputSlots;
    }

    private boolean canInsertOutput(ItemStack result) {
        int slot = outputSlot();
        int limit = inventory.getSlotLimit(slot);
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            return result.getCount() <= Math.min(result.getMaxStackSize(), limit);
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= Math.min(current.getMaxStackSize(), limit);
    }

    private void finishCraft(Craft craft) {
        for (int i = 0; i < craft.slots().length; i++) {
            inventory.extractItem(craft.slots()[i], craft.counts()[i], false);
        }
        int slot = outputSlot();
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(slot, craft.result().copy());
        } else {
            ItemStack grown = current.copy();
            grown.grow(craft.result().getCount());
            inventory.setStackInSlot(slot, grown);
        }
    }

    /** Drops all items; called when the block is broken. */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
    }

    public int getInputSlots() {
        return inputSlots;
    }

    public ModEnergyStorage getEnergyStorage() {
        return energy;
    }

    /** Hopper and conveyor view: inserts into input slots, extracts from the output slot only. */
    public IItemHandler getItemView() {
        return itemView;
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
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        energy.setEnergy(tag.getInt("energy"));
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineMenu(menuType(), containerId, playerInventory, getBlockState().getBlock(),
                inputSlots, inventory, dataAccess, ContainerLevelAccess.create(level, worldPosition));
    }

    /** Hopper and conveyor view of the inventory. */
    private final class ItemView implements IItemHandler {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot).copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= inputSlots) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != outputSlot()) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < inputSlots && isValidInput(stack);
        }
    }
}
