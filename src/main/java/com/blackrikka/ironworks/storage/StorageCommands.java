package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.Ironworks;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

/**
 * Debug command for storage networks (OP only): {@code /ironworks storage <x y z>}. Prints the network found from
 * the given node: online state, number of racks and nodes, and the stored amount per kind.
 */
@EventBusSubscriber(modid = Ironworks.MOD_ID)
public final class StorageCommands {
    private StorageCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal(Ironworks.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("storage")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> report(context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
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
}
