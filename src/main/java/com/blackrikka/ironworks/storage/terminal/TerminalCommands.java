package com.blackrikka.ironworks.storage.terminal;

import com.blackrikka.ironworks.net.InsertRequestPayload;
import com.blackrikka.ironworks.net.QueryRequestPayload;
import com.blackrikka.ironworks.net.QueryResultPayload;
import com.blackrikka.ironworks.net.TakeRequestPayload;
import com.blackrikka.ironworks.registry.ModItems;
import com.blackrikka.ironworks.registry.ModMenus;
import com.blackrikka.ironworks.storage.ItemKey;
import com.blackrikka.ironworks.storage.StorageNetwork;
import com.blackrikka.ironworks.storage.query.QueryPage;
import com.blackrikka.ironworks.storage.query.SortMode;
import com.blackrikka.ironworks.storage.terminal.TerminalActions.InsertMode;
import com.blackrikka.ironworks.storage.terminal.TerminalActions.Outcome;
import com.blackrikka.ironworks.storage.terminal.TerminalActions.TakeMode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Debug commands for the DB terminal (OP only), registered under {@code /ironworks terminal}:
 * <ul>
 *   <li>{@code take <x y z> <item> <STACK|HALF|ONE|SHIFT> [far|closed]}: runs {@link TerminalActions#take} for a fake
 *       player who has the terminal open, and prints the result.</li>
 *   <li>{@code insert <x y z> <item> <amount> <ALL|ONE> [far|closed]}: puts {@code amount} on the fake player's
 *       cursor, then runs {@link TerminalActions#insert}.</li>
 *   <li>{@code fill full|empty}: fills or empties the fake player's main inventory (cobblestone x64).</li>
 *   <li>{@code fill cursor <item> <amount>} / {@code fill cursor clear}: the cursor that take commands start from.</li>
 *   <li>{@code codec}: encodes and decodes every terminal payload with the server's registries and compares them.</li>
 * </ul>
 * {@code far} puts the fake player {@value #FAR_DISTANCE} blocks from the terminal, {@code closed} leaves the
 * terminal menu unopened. Both should be refused. A take starts from the cursor set by {@code fill cursor};
 * an insert puts its own amount on the cursor. Neither command changes the cursor set by {@code fill cursor}.
 */
public final class TerminalCommands {
    private static final GameProfile TEST_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("ironworks-terminal-test".getBytes(StandardCharsets.UTF_8)), "IronworksTest");
    private static final double FAR_DISTANCE = 20.0;

    /** Cursor the take commands start from; set with {@code fill cursor}. */
    private static ItemStack testCursor = ItemStack.EMPTY;

    private TerminalCommands() {
    }

    /** Where the fake player stands and whether the terminal is open. */
    private enum Setup {
        NEAR, FAR, CLOSED
    }

    /** The {@code terminal} branch, added to the {@code /ironworks} command by {@code StorageCommands}. */
    public static LiteralArgumentBuilder<CommandSourceStack> node(CommandBuildContext buildContext) {
        return Commands.literal("terminal")
                .then(Commands.literal("take")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .executes(context -> take(context, Setup.NEAR))
                                                .then(Commands.literal("far")
                                                        .executes(context -> take(context, Setup.FAR)))
                                                .then(Commands.literal("closed")
                                                        .executes(context -> take(context, Setup.CLOSED)))))))
                .then(Commands.literal("insert")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                .then(Commands.argument("mode", StringArgumentType.word())
                                                        .executes(context -> insert(context, Setup.NEAR))
                                                        .then(Commands.literal("far")
                                                                .executes(context -> insert(context, Setup.FAR)))
                                                        .then(Commands.literal("closed")
                                                                .executes(context -> insert(context, Setup.CLOSED))))))))
                .then(Commands.literal("fill")
                        .then(Commands.literal("full")
                                .executes(context -> fillInventory(context.getSource(), true)))
                        .then(Commands.literal("empty")
                                .executes(context -> fillInventory(context.getSource(), false)))
                        .then(Commands.literal("cursor")
                                .then(Commands.literal("clear")
                                        .executes(context -> setCursor(context.getSource(), ItemStack.EMPTY)))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(context -> setCursor(context.getSource(),
                                                        cursorFrom(context)))))))
                .then(Commands.literal("codec")
                        .executes(context -> codec(context.getSource())));
    }

    private static int take(CommandContext<CommandSourceStack> context, Setup setup) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ItemStack template = ItemArgument.getItem(context, "item").createItemStack(1, false);
        String modeText = StringArgumentType.getString(context, "mode");
        TakeMode mode = parseTakeMode(modeText);
        if (mode == null) {
            source.sendFailure(Component.literal("mode must be STACK, HALF, ONE or SHIFT"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        FakePlayer player = fakePlayer(level);
        DbTerminalMenu menu = prepare(player, pos, setup, testCursor);
        ItemKey key = new ItemKey(template);
        long netBefore = storedIn(level, pos, key);
        long invBefore = inInventory(player, key);
        ItemStack cursorBefore = menu.getCarried().copy();

        Outcome outcome = TerminalActions.take(player, template, mode);

        List<String> lines = new ArrayList<>();
        lines.add("[terminal take] mode=" + mode + " item=" + itemId(template) + " setup=" + setup.name().toLowerCase(Locale.ROOT)
                + " success=" + outcome.success() + " amount=" + outcome.amount());
        lines.add("  network " + netBefore + " -> " + storedIn(level, pos, key)
                + "; inventory " + invBefore + " -> " + inInventory(player, key)
                + "; cursor " + describe(cursorBefore) + " -> " + describe(menu.getCarried()));
        if (!outcome.success()) {
            lines.add("  reason: " + outcome.message().getString());
        }
        say(source, lines);
        return outcome.success() ? 1 : 0;
    }

    private static int insert(CommandContext<CommandSourceStack> context, Setup setup) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ItemStack template = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        String modeText = StringArgumentType.getString(context, "mode").toUpperCase(Locale.ROOT);
        InsertMode mode = switch (modeText) {
            case "ALL" -> InsertMode.ALL;
            case "ONE" -> InsertMode.ONE;
            default -> null;
        };
        if (mode == null) {
            source.sendFailure(Component.literal("mode must be ALL or ONE"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        FakePlayer player = fakePlayer(level);
        DbTerminalMenu menu = prepare(player, pos, setup, template.copyWithCount(amount));
        ItemKey key = new ItemKey(template);
        long netBefore = storedIn(level, pos, key);
        ItemStack cursorBefore = menu.getCarried().copy();

        Outcome outcome = TerminalActions.insert(player, mode);

        List<String> lines = new ArrayList<>();
        lines.add("[terminal insert] mode=" + mode + " item=" + itemId(template) + " setup=" + setup.name().toLowerCase(Locale.ROOT)
                + " success=" + outcome.success() + " amount=" + outcome.amount());
        lines.add("  network " + netBefore + " -> " + storedIn(level, pos, key)
                + "; cursor " + describe(cursorBefore) + " -> " + describe(menu.getCarried()));
        if (!outcome.success()) {
            lines.add("  reason: " + outcome.message().getString());
        }
        say(source, lines);
        return outcome.success() ? 1 : 0;
    }

    private static int fillInventory(CommandSourceStack source, boolean full) {
        Inventory inventory = fakePlayer(source.getLevel()).getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            inventory.items.set(i, full ? new ItemStack(Items.COBBLESTONE, 64) : ItemStack.EMPTY);
        }
        say(source, List.of("[terminal fill] fake player inventory " + (full ? "filled with cobblestone x64" : "emptied")));
        return 1;
    }

    private static ItemStack cursorFrom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack template = ItemArgument.getItem(context, "item").createItemStack(1, false);
        return template.copyWithCount(IntegerArgumentType.getInteger(context, "amount"));
    }

    private static int setCursor(CommandSourceStack source, ItemStack cursor) {
        testCursor = cursor.copy();
        say(source, List.of("[terminal fill] test cursor = " + describe(testCursor)));
        return 1;
    }

    /** Round-trips every terminal payload through the stream codec and reports whether each decoded copy is equal. */
    private static int codec(CommandSourceStack source) {
        RegistryAccess access = source.getServer().registryAccess();
        List<QueryPage.Entry> entries = List.of(
                new QueryPage.Entry(new ItemStack(Items.IRON_ORE), 1200),
                new QueryPage.Entry(new ItemStack(Items.ENDER_PEARL), 40),
                new QueryPage.Entry(new ItemStack(ModItems.DRIVE_4K.get()), 3));
        List<String> lines = new ArrayList<>();
        lines.add(check("QueryRequestPayload", QueryRequestPayload.STREAM_CODEC,
                new QueryRequestPayload("iron @minecraft #c:ores >=10", SortMode.COUNT, true, 2), access, Objects::equals));
        lines.add(check("QueryRequestPayload (empty)", QueryRequestPayload.STREAM_CODEC,
                new QueryRequestPayload("", SortMode.NAME, false, 0), access, Objects::equals));
        lines.add(check("QueryResultPayload", QueryResultPayload.STREAM_CODEC,
                new QueryResultPayload(new QueryPage(entries, 3, 0, 1, true, 1243, 2, List.of("bad token: x"))), access,
                (a, b) -> samePage(a.page(), b.page())));
        lines.add(check("QueryResultPayload (offline)", QueryResultPayload.STREAM_CODEC,
                new QueryResultPayload(new QueryPage(List.of(), 0, 0, 1, false, 0, 0, List.of())), access,
                (a, b) -> samePage(a.page(), b.page())));
        lines.add(check("TakeRequestPayload", TakeRequestPayload.STREAM_CODEC,
                new TakeRequestPayload(new ItemStack(Items.IRON_ORE), TakeMode.SHIFT_TO_INVENTORY), access,
                (a, b) -> a.mode() == b.mode() && ItemStack.matches(a.template(), b.template())));
        lines.add(check("InsertRequestPayload", InsertRequestPayload.STREAM_CODEC,
                new InsertRequestPayload(InsertMode.ONE), access, Objects::equals));
        say(source, lines);
        return 1;
    }

    /**
     * Encodes {@code value}, decodes it again and compares with {@code same}. ItemStack has no equals in this version,
     * so the callers compare stacks with {@link ItemStack#matches} instead.
     */
    private static <T extends CustomPacketPayload> String check(String name, StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                                T value, RegistryAccess access,
                                                                BiPredicate<T, T> same) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        codec.encode(buf, value);
        int bytes = buf.readableBytes();
        T decoded = codec.decode(buf);
        boolean equal = same.test(value, decoded) && buf.readableBytes() == 0;
        return "[terminal codec] " + name + ": " + (equal ? "OK" : "MISMATCH") + " (" + bytes + " bytes)";
    }

    private static boolean samePage(QueryPage a, QueryPage b) {
        if (a.totalMatches() != b.totalMatches() || a.page() != b.page() || a.pageCount() != b.pageCount()
                || a.online() != b.online() || a.usedItems() != b.usedItems() || a.rackCount() != b.rackCount()
                || !a.errors().equals(b.errors()) || a.entries().size() != b.entries().size()) {
            return false;
        }
        for (int i = 0; i < a.entries().size(); i++) {
            QueryPage.Entry x = a.entries().get(i);
            QueryPage.Entry y = b.entries().get(i);
            if (x.count() != y.count() || !ItemStack.matches(x.template(), y.template())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gives the fake player a fresh terminal menu at {@code pos} with {@code cursor} on the cursor. For {@link Setup#CLOSED}
     * the terminal is not the open menu, so the actions must refuse.
     */
    private static DbTerminalMenu prepare(FakePlayer player, BlockPos pos, Setup setup, ItemStack cursor) {
        double x = pos.getX() + 0.5 + (setup == Setup.FAR ? FAR_DISTANCE : 0.0);
        player.setPos(x, pos.getY() + 1.0, pos.getZ() + 0.5);
        DbTerminalMenu menu = new DbTerminalMenu(ModMenus.DB_TERMINAL_MENU.get(), 900, player.getInventory(), pos);
        menu.setCarried(cursor.copy());
        player.containerMenu = setup == Setup.CLOSED ? player.inventoryMenu : menu;
        return menu;
    }

    private static FakePlayer fakePlayer(ServerLevel level) {
        return FakePlayerFactory.get(level, TEST_PROFILE);
    }

    private static TakeMode parseTakeMode(String text) {
        return switch (text.toUpperCase(Locale.ROOT)) {
            case "STACK" -> TakeMode.STACK;
            case "HALF" -> TakeMode.HALF;
            case "ONE" -> TakeMode.ONE;
            case "SHIFT", "SHIFT_TO_INVENTORY" -> TakeMode.SHIFT_TO_INVENTORY;
            default -> null;
        };
    }

    private static long storedIn(ServerLevel level, BlockPos pos, ItemKey key) {
        return StorageNetwork.collect(level, pos).snapshot().getOrDefault(key, 0L);
    }

    private static long inInventory(FakePlayer player, ItemKey key) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (key.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static String itemId(ItemStack stack) {
        Item item = stack.getItem();
        return String.valueOf(BuiltInRegistries.ITEM.getKey(item));
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getCount() + " x " + itemId(stack);
    }

    private static void say(CommandSourceStack source, List<String> lines) {
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
    }
}
