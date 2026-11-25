package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.recipe.MachineRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Shared container for all machines. Slot order: input slots (0 .. inputSlots-1), output slot, main inventory,
 * hotbar. Input slots are laid out in a row centred on {@link #INPUT_CENTER_X}; the output slot sits at
 * {@link #OUTPUT_X}. Data sync indices: 0 energy, 1 max energy, 2 progress, 3 max progress.
 */
public class MachineMenu extends AbstractContainerMenu {
    public static final int INPUT_CENTER_X = 56;
    public static final int SLOT_Y = 35;
    public static final int OUTPUT_X = 116;
    public static final int PLAYER_INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final Block block;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int inputSlots;
    private final int outputSlot;
    private final int playerStart;
    private final int hotbarStart;
    private final int playerEnd;

    /** Left column of the input layout for two and four inputs (the single-input layout is centred). */
    public static final int INPUT_GRID_X = 38;

    /**
     * X position of input slot {@code index} out of {@code count}. One input is centred on {@link #INPUT_CENTER_X}.
     * Two inputs sit side by side from {@link #INPUT_GRID_X}. Four inputs form a 2x2 grid (see {@link #inputSlotY}).
     * All layouts stay left of the progress arrow at x=79.
     */
    public static int inputSlotX(int index, int count) {
        if (count == 4) {
            return INPUT_GRID_X + (index % 2) * 18;
        }
        if (count == 2) {
            return INPUT_GRID_X + index * 18;
        }
        return INPUT_CENTER_X + index * 18 - (count - 1) * 9;
    }

    /** Y position of input slot {@code index} out of {@code count}: a 2x2 grid uses rows at y=26 and y=44. */
    public static int inputSlotY(int index, int count) {
        if (count == 4) {
            return index < 2 ? SLOT_Y - 9 : SLOT_Y + 9;
        }
        return SLOT_Y;
    }

    /** Client-side constructor, called by the menu type extension. The server writes the block position first. */
    public static MachineMenu fromNetwork(MenuType<?> type, int containerId, Inventory inventory,
                                          RegistryFriendlyByteBuf buf, Block block, int inputSlots) {
        return new MachineMenu(type, containerId, inventory, block, inputSlots,
                new ItemStackHandler(inputSlots + 1),
                new SimpleContainerData(MachineBlockEntity.DATA_COUNT),
                ContainerLevelAccess.create(inventory.player.level(), buf.readBlockPos()));
    }

    /** Server-side constructor, called by the block entity with its real inventory and data. */
    public MachineMenu(MenuType<?> type, int containerId, Inventory inventory, Block block, int inputSlots,
                       ItemStackHandler handler, ContainerData data, ContainerLevelAccess access) {
        super(type, containerId);
        this.block = block;
        this.access = access;
        this.data = data;
        this.inputSlots = inputSlots;
        this.outputSlot = inputSlots;
        this.playerStart = inputSlots + 1;
        this.hotbarStart = playerStart + 27;
        this.playerEnd = hotbarStart + 9;

        for (int i = 0; i < inputSlots; i++) {
            this.addSlot(new SlotItemHandler(handler, i, inputSlotX(i, inputSlots), inputSlotY(i, inputSlots)));
        }
        this.addSlot(new OutputSlot(handler, outputSlot, OUTPUT_X, SLOT_Y));
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
        this.addDataSlots(data);
    }

    public int getInputSlots() {
        return inputSlots;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    public int getProgress() {
        return data.get(2);
    }

    public int getMaxProgress() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index <= outputSlot) {
            // Machine slots -> player inventory.
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, inputSlots, false)) {
            // Player inventory -> input slots (moveItemStackTo respects Slot.mayPlace), else shuffle inside the player inventory.
            if (index < hotbarStart) {
                if (!this.moveItemStackTo(stack, hotbarStart, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, block);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    /** Output slot: players may only take items out. */
    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
