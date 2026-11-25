package com.blackrikka.ironworks.storage.terminal;

import com.blackrikka.ironworks.storage.ItemKey;
import com.blackrikka.ironworks.storage.StorageNetwork;
import com.blackrikka.ironworks.storage.query.QueryPage;
import com.blackrikka.ironworks.storage.query.QueryRequest;
import com.blackrikka.ironworks.storage.query.QueryService;
import com.blackrikka.ironworks.storage.query.SortMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side terminal operations: query, take into the cursor or the inventory, and put the cursor into the network.
 * Packet handlers and the debug commands both call these methods. Server only.
 * <p>
 * Every operation checks that the player has a {@link DbTerminalMenu} open and that it is still usable (the terminal
 * block is there and the player is within {@link #MAX_REACH} blocks). Amounts are computed here, never taken from the
 * client, and the take path checks the kind against the network snapshot.
 */
public final class TerminalActions {
    /** Maximum distance from the player to the terminal, in blocks. */
    public static final double MAX_REACH = 8.0;
    /** Longest accepted query text, in characters. */
    public static final int MAX_QUERY_LENGTH = 128;

    /** How a result slot is taken. */
    public enum TakeMode {
        /** One full stack (the kind's max stack size) onto the cursor. */
        STACK,
        /** Half of a full stack (rounded up) onto the cursor. */
        HALF,
        /** One item onto the cursor. */
        ONE,
        /** As much as fits of one full stack into the player inventory; the rest stays in the network. */
        SHIFT_TO_INVENTORY
    }

    /** How the cursor stack is put back into the network. */
    public enum InsertMode {
        /** The whole cursor stack. */
        ALL,
        /** One item from the cursor. */
        ONE
    }

    /**
     * Result of one take or insert.
     *
     * @param success true if something moved
     * @param amount the number of items moved
     * @param message the reason when {@code success} is false (translation key), empty otherwise
     */
    public record Outcome(boolean success, long amount, Component message) {
    }

    private TerminalActions() {
    }

    /** True if the block centre of {@code pos} is within {@link #MAX_REACH} of the player. */
    public static boolean withinReach(Player player, BlockPos pos) {
        double reach = MAX_REACH * MAX_REACH;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= reach;
    }

    /** The terminal menu the player has open, if it is still usable; otherwise null. */
    public static DbTerminalMenu openTerminal(Player player) {
        return player.containerMenu instanceof DbTerminalMenu menu && menu.stillValid(player) ? menu : null;
    }

    /**
     * Runs a search for the open terminal.
     *
     * @param text the query text; longer than {@link #MAX_QUERY_LENGTH} is cut and reported in the errors
     * @return the page, or an empty offline page with an error if no terminal is usable
     */
    public static QueryPage query(ServerPlayer player, String text, SortMode sort, boolean descending, int pageIndex) {
        DbTerminalMenu menu = openTerminal(player);
        if (menu == null) {
            return new QueryPage(List.of(), 0, 0, 1, false, 0, 0, List.of("terminal is not open or out of reach"));
        }
        String query = text == null ? "" : text;
        List<String> extraErrors = new ArrayList<>();
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
            extraErrors.add("query is longer than " + MAX_QUERY_LENGTH + " characters and was cut");
        }
        StorageNetwork network = StorageNetwork.collect((ServerLevel) player.level(), menu.getPos());
        QueryPage page = QueryService.run(network,
                new QueryRequest(query, sort, descending, pageIndex, QueryService.PAGE_SIZE));
        if (extraErrors.isEmpty()) {
            return page;
        }
        List<String> errors = new ArrayList<>(extraErrors);
        errors.addAll(page.errors());
        return new QueryPage(page.entries(), page.totalMatches(), page.page(), page.pageCount(), page.online(),
                page.usedItems(), page.rackCount(), errors);
    }

    /**
     * Takes a kind out of the network. STACK, HALF and ONE go to the cursor; SHIFT_TO_INVENTORY goes to the inventory.
     * The amount is limited by the cursor room (or the inventory room), the stored amount and the stack size.
     */
    public static Outcome take(ServerPlayer player, ItemStack template, TakeMode mode) {
        DbTerminalMenu menu = openTerminal(player);
        if (menu == null) {
            return fail("message.ironworks.terminal.not_open");
        }
        if (template.isEmpty() || mode == null) {
            return fail("message.ironworks.terminal.not_stored");
        }
        StorageNetwork network = StorageNetwork.collect((ServerLevel) player.level(), menu.getPos());
        if (!network.isOnline()) {
            return fail("message.ironworks.terminal.offline");
        }
        ItemKey key = new ItemKey(template);
        long available = network.snapshot().getOrDefault(key, 0L);
        if (available <= 0) {
            return fail("message.ironworks.terminal.not_stored");
        }
        int maxStack = Math.max(1, key.stack().getMaxStackSize());
        if (mode == TakeMode.SHIFT_TO_INVENTORY) {
            return takeToInventory(player, network, key, maxStack, available);
        }

        ItemStack cursor = menu.getCarried();
        if (!cursor.isEmpty() && !key.matches(cursor)) {
            return fail("message.ironworks.terminal.cursor_busy");
        }
        long held = cursor.getCount();
        long room = Math.max(0, maxStack - held);
        long wanted = switch (mode) {
            case STACK -> maxStack;
            case HALF -> (maxStack + 1) / 2;
            default -> 1;
        };
        long amount = Math.min(Math.min(wanted, room), available);
        if (amount <= 0) {
            return fail("message.ironworks.terminal.cursor_full");
        }
        long taken = network.extract(key.stack(), amount, false);
        if (taken <= 0) {
            return fail("message.ironworks.terminal.not_stored");
        }
        menu.setCarried(key.stack().copyWithCount((int) (held + taken)));
        return ok(taken);
    }

    /**
     * Puts the cursor stack (or one item of it) into the network. The cursor keeps whatever the network refuses.
     */
    public static Outcome insert(ServerPlayer player, InsertMode mode) {
        DbTerminalMenu menu = openTerminal(player);
        if (menu == null) {
            return fail("message.ironworks.terminal.not_open");
        }
        if (mode == null) {
            return fail("message.ironworks.terminal.not_accepted");
        }
        ItemStack cursor = menu.getCarried();
        if (cursor.isEmpty()) {
            return fail("message.ironworks.terminal.cursor_empty");
        }
        StorageNetwork network = StorageNetwork.collect((ServerLevel) player.level(), menu.getPos());
        if (!network.isOnline()) {
            return fail("message.ironworks.terminal.offline");
        }
        long wanted = mode == InsertMode.ONE ? 1 : cursor.getCount();
        long accepted = network.insert(cursor, wanted, false);
        if (accepted <= 0) {
            return fail("message.ironworks.terminal.not_accepted");
        }
        int left = cursor.getCount() - (int) accepted;
        menu.setCarried(left > 0 ? cursor.copyWithCount(left) : ItemStack.EMPTY);
        return ok(accepted);
    }

    /**
     * Shift-click from the player inventory: puts as much of {@code stack} as the network accepts and shrinks
     * {@code stack} by that amount. Used by {@link DbTerminalMenu#quickMoveStack}.
     *
     * @return the amount accepted
     */
    public static long depositStack(Player player, DbTerminalMenu menu, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level) || stack.isEmpty() || !menu.stillValid(player)) {
            return 0;
        }
        long accepted = StorageNetwork.collect(level, menu.getPos()).insert(stack, stack.getCount(), false);
        if (accepted > 0) {
            stack.shrink((int) accepted);
        }
        return accepted;
    }

    private static Outcome takeToInventory(ServerPlayer player, StorageNetwork network, ItemKey key, int maxStack,
                                           long available) {
        Inventory inventory = player.getInventory();
        long room = inventoryRoom(inventory, key, maxStack);
        long amount = Math.min(Math.min(maxStack, room), available);
        if (amount <= 0) {
            return fail("message.ironworks.terminal.inventory_full");
        }
        long taken = network.extract(key.stack(), amount, false);
        if (taken <= 0) {
            return fail("message.ironworks.terminal.not_stored");
        }
        ItemStack stack = key.stack().copyWithCount((int) taken);
        inventory.add(stack);
        long moved = taken;
        if (!stack.isEmpty()) {
            // The room check above makes this unlikely; anything left goes back into the network, then to the ground.
            long back = network.insert(stack, stack.getCount(), false);
            long lost = stack.getCount() - back;
            if (lost > 0) {
                player.drop(key.stack().copyWithCount((int) lost), false);
                moved -= lost;
            }
        }
        return ok(moved);
    }

    /** Items of {@code key}'s kind that the main inventory can still take, given {@code maxStack} per slot. */
    private static long inventoryRoom(Inventory inventory, ItemKey key, int maxStack) {
        long room = 0;
        for (ItemStack slot : inventory.items) {
            if (slot.isEmpty()) {
                room += maxStack;
            } else if (key.matches(slot)) {
                room += Math.max(0, maxStack - slot.getCount());
            }
        }
        return room;
    }

    private static Outcome ok(long amount) {
        return new Outcome(true, amount, Component.empty());
    }

    private static Outcome fail(String translationKey) {
        return new Outcome(false, 0, Component.translatable(translationKey));
    }
}
