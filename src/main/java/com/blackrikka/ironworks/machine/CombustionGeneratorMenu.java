package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlocks;
import com.blackrikka.ironworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container for the combustion generator: one fuel slot plus the player inventory.
 * Energy and burn progress are synced through {@link ContainerData} (indices: 0 energy, 1 max energy,
 * 2 burn time left, 3 burn total).
 */
public class CombustionGeneratorMenu extends AbstractContainerMenu {
    /** Slot index range (menu slots): fuel is 0, main inventory 1..27, hotbar 28..36. */
    private static final int FUEL_SLOT = 0;
    private static final int MAIN_START = 1;
    private static final int HOTBAR_START = 28;
    private static final int PLAYER_END = 37;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by the menu type extension. The server writes the block position first. */
    public CombustionGeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory,
                new ItemStackHandler(1) {
                    @Override
                    public boolean isItemValid(int slot, ItemStack stack) {
                        return CombustionGeneratorBlockEntity.isFuel(stack);
                    }
                },
                new SimpleContainerData(4),
                ContainerLevelAccess.create(inventory.player.level(), buf.readBlockPos()));
    }

    /** Server-side constructor, called by the block entity with its real inventory and data. */
    public CombustionGeneratorMenu(int containerId, Inventory inventory, ItemStackHandler fuel,
                                   ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.COMBUSTION_GENERATOR_MENU.get(), containerId);
        this.data = data;
        this.access = access;

        this.addSlot(new SlotItemHandler(fuel, FUEL_SLOT, 80, 35));
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
        this.addDataSlots(data);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    public int getBurnTime() {
        return data.get(2);
    }

    public int getBurnTotal() {
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

        if (index == FUEL_SLOT) {
            // Fuel slot -> player inventory.
            if (!this.moveItemStackTo(stack, MAIN_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CombustionGeneratorBlockEntity.isFuel(stack)) {
            // Player inventory -> fuel slot (only if the slot accepts it).
            if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < HOTBAR_START) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, MAIN_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
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
        return stillValid(this.access, player, ModBlocks.COMBUSTION_GENERATOR.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }
}
