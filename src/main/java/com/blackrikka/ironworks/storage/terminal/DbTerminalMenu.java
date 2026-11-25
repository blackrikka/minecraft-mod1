package com.blackrikka.ironworks.storage.terminal;

import com.blackrikka.ironworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * DB terminal GUI. The menu holds no container of its own: the only slots are the player's inventory, and the
 * search results are sent by packets. Shift-clicking a player slot puts the stack into the network.
 * Slot order: main inventory (0 .. 26), hotbar (27 .. 35).
 */
public class DbTerminalMenu extends AbstractContainerMenu {
    /** Top of the player's main inventory rows, relative to the GUI. */
    public static final int PLAYER_INVENTORY_Y = 206;
    /** Top of the hotbar row, relative to the GUI. */
    public static final int HOTBAR_Y = 264;

    /** Slot indices: the player's 36 inventory slots (main inventory, then hotbar). */
    private static final int PLAYER_START = 0;
    private static final int PLAYER_END = 36;

    private final BlockPos pos;

    /** Client-side constructor, called by the menu type extension. The server writes the block position first. */
    public static DbTerminalMenu fromNetwork(MenuType<?> type, int containerId, Inventory inventory,
                                             RegistryFriendlyByteBuf buf) {
        return new DbTerminalMenu(type, containerId, inventory, buf.readBlockPos());
    }

    public DbTerminalMenu(MenuType<?> type, int containerId, Inventory inventory, BlockPos pos) {
        super(type, containerId);
        this.pos = pos.immutable();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    /** The terminal block this menu belongs to. */
    public BlockPos getPos() {
        return pos;
    }

    /** True while the terminal block is still at {@link #getPos()} and the player is within reach. */
    @Override
    public boolean stillValid(Player player) {
        return player.level().isLoaded(pos)
                && player.level().getBlockState(pos).is(ModBlocks.DB_TERMINAL.get())
                && TerminalActions.withinReach(player, pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PLAYER_START || index >= PLAYER_END) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        long accepted = TerminalActions.depositStack(player, this, stack);
        if (accepted <= 0) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
