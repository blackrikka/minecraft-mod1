package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.storage.query.QueryPage;
import com.blackrikka.ironworks.storage.query.QueryRequest;
import com.blackrikka.ironworks.storage.query.QueryService;
import com.blackrikka.ironworks.storage.query.SortMode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Debug commands for storage networks (OP only):
 * <ul>
 *   <li>{@code /ironworks storage <x y z>}: prints the network found from the given node: online state, number of
 *       racks and nodes, and the stored amount per kind.</li>
 *   <li>{@code /ironworks query <x y z> [query] [sort=name|count|id] [asc|desc] [page=N]}: runs the DB query
 *       (see {@code DbQuery}) and prints one line per kind on the page, then a summary. Pages start at 0.</li>
 *   <li>{@code /ironworks extract <x y z> <item> <amount>}: takes items out of the network and drops them above
 *       the given position.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Ironworks.MOD_ID)
public final class StorageCommands {
    private StorageCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();
        dispatcher.register(Commands.literal(Ironworks.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("storage")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> report(context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("query")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> query(context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos"), ""))
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(context -> query(context.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "query"))))))
                .then(Commands.literal("extract")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> extract(context.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                        ItemArgument.getItem(context, "item")
                                                                .createItemStack(1, false),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))));
    }

    private static int report(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        StorageNetwork network = StorageNetwork.collect(level, pos);
        if (network.nodeCount() == 0) {
            source.sendFailure(Component.literal("No storage network node at "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
            return 0;
        }

        long onlineRacks = network.providers().stream().filter(IStorageProvider::isOnline).count();
        source.sendSuccess(() -> Component.literal("Network: online=" + network.isOnline()
                + ", nodes=" + network.nodeCount()
                + ", racks=" + network.providers().size()
                + " (online " + onlineRacks + ")"), false);

        Map<ItemKey, Long> snapshot = network.snapshot();
        long total = 0;
        for (Map.Entry<ItemKey, Long> entry : snapshot.entrySet()) {
            total += entry.getValue();
            String id = String.valueOf(BuiltInRegistries.ITEM.getKey(entry.getKey().stack().getItem()));
            source.sendSuccess(() -> Component.literal("  " + id + " x " + entry.getValue()), false);
        }
        long kinds = snapshot.size();
        long finalTotal = total;
        source.sendSuccess(() -> Component.literal("Stored: " + finalTotal + " items, " + kinds + " kinds"), false);
        return 1;
    }

    /**
     * Runs a query on the network at {@code pos}. Words are the query; {@code sort=...}, an optional {@code asc} or
     * {@code desc} right after it, and {@code page=N} are options and are taken out of the text first.
     */
    private static int query(CommandSourceStack source, BlockPos pos, String text) {
        StorageNetwork network = StorageNetwork.collect(source.getLevel(), pos);
        if (network.nodeCount() == 0) {
            source.sendFailure(Component.literal("No storage network node at "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
            return 0;
        }

        List<String> words = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        SortMode sort = SortMode.NAME;
        boolean descending = false;
        int page = 0;
        boolean afterSort = false;
        for (String token : text.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (afterSort && (lower.equals("asc") || lower.equals("desc"))) {
                descending = lower.equals("desc");
                afterSort = false;
                continue;
            }
            afterSort = false;
            if (lower.startsWith("sort=")) {
                afterSort = true;
                switch (lower.substring(5)) {
                    case "name" -> sort = SortMode.NAME;
                    case "count" -> sort = SortMode.COUNT;
                    case "id" -> sort = SortMode.ID;
                    default -> errors.add(token + ": sort must be name, count or id");
                }
            } else if (lower.startsWith("page=")) {
                try {
                    page = Integer.parseInt(lower.substring(5));
                    if (page < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    errors.add(token + ": page must be 0 or more");
                    page = 0;
                }
            } else {
                words.add(token);
            }
        }

        QueryPage result = QueryService.run(network,
                new QueryRequest(String.join(" ", words), sort, descending, page, QueryService.PAGE_SIZE));
        for (QueryPage.Entry entry : result.entries()) {
            String id = String.valueOf(BuiltInRegistries.ITEM.getKey(entry.template().getItem()));
            source.sendSuccess(() -> Component.literal(id + " x " + entry.count()), false);
        }
        errors.addAll(result.errors());
        for (String error : errors) {
            source.sendSuccess(() -> Component.literal("  error: " + error), false);
        }
        String state = result.online() ? "online" : "offline";
        String summary = "total " + result.totalMatches() + " kinds / page " + result.page()
                + "/" + (result.pageCount() - 1) + " / " + state + " / errors " + errors.size();
        source.sendSuccess(() -> Component.literal(summary), false);
        return result.totalMatches();
    }

    /** Takes up to {@code amount} of {@code template}'s kind out of the network and drops it above {@code pos}. */
    private static int extract(CommandSourceStack source, BlockPos pos, ItemStack template, int amount) {
        ServerLevel level = source.getLevel();
        StorageNetwork network = StorageNetwork.collect(level, pos);
        if (network.nodeCount() == 0) {
            source.sendFailure(Component.literal("No storage network node at "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
            return 0;
        }

        long taken = QueryService.extract(network, template, amount);
        int maxStack = Math.max(1, template.getMaxStackSize());
        long left = taken;
        while (left > 0) {
            int size = (int) Math.min(maxStack, left);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    template.copyWithCount(size));
            level.addFreshEntity(drop);
            left -= size;
        }

        String id = String.valueOf(BuiltInRegistries.ITEM.getKey(template.getItem()));
        source.sendSuccess(() -> Component.literal("Extracted " + taken + " x " + id
                + " (requested " + amount + ")"), false);
        return (int) taken;
    }
}
