package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.registry.ModBlocks;
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
 * Server rack GUI. Slot order: drive slots (0 .. 7, 4 per row, 2 rows), main inventory, hotbar.
 * Data sync indices: 0 energy, 1 max energy, 2 online.
 */
public class ServerRackMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 52;
    public static final int SLOT_Y = 26;
    public static final int COLUMNS = 4;
    public static final int PLAYER_INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final Block block;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int playerStart;
    private final int hotbarStart;
    private final int playerEnd;

    /** Client-side constructor, called by the menu type extension. The server writes the block position first. */
    public static ServerRackMenu fromNetwork(MenuType<?> type, int containerId, Inventory inventory,
                                             RegistryFriendlyByteBuf buf) {
        // Client-side drive filter mirrors the server's so the client never offers a non-drive stack.
        ItemStackHandler handler = new ItemStackHandler(ServerRackBlockEntity.SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return ServerRackBlockEntity.isDrive(stack);
            }
        };
        return new ServerRackMenu(type, containerId, inventory, handler,
                new SimpleContainerData(ServerRackBlockEntity.DATA_COUNT), ModBlocks.SERVER_RACK.get(),
                ContainerLevelAccess.create(inventory.player.level(), buf.readBlockPos()));
    }

    /** Server-side constructor, called by the block entity with its real drives and data. */
    public ServerRackMenu(MenuType<?> type, int containerId, Inventory inventory, ItemStackHandler drives,
                          ContainerData data, Block block, ContainerLevelAccess access) {
        super(type, containerId);
        this.block = block;
        this.access = access;
        this.data = data;
        this.playerStart = ServerRackBlockEntity.SLOTS;
        this.hotbarStart = playerStart + 27;
        this.playerEnd = hotbarStart + 9;

        for (int i = 0; i < ServerRackBlockEntity.SLOTS; i++) {
            this.addSlot(new SlotItemHandler(drives, i, SLOT_X + (i % COLUMNS) * 18, SLOT_Y + (i / COLUMNS) * 18));
        }
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
        this.addDataSlots(data);
    }

    public boolean isOnline() {
        return data.get(2) != 0;
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
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

        if (index < playerStart) {
            // Drive slots -> player inventory.
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, playerStart, false)) {
            // Player inventory -> drive slots (moveItemStackTo respects Slot.mayPlace), else shuffle inside it.
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
}
