package com.blackrikka.ironworks.net;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.storage.query.QueryPage;
import com.blackrikka.ironworks.storage.terminal.TerminalActions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Terminal payload registration (mod event bus). The handlers are thin: they call {@link TerminalActions} on the
 * server and report failures to the player. The client handler is referenced only inside a lambda, so a dedicated
 * server never loads the client classes.
 */
public final class TerminalNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private TerminalNetwork() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Ironworks.MOD_ID, path);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToServer(QueryRequestPayload.TYPE, QueryRequestPayload.STREAM_CODEC, TerminalNetwork::handleQuery)
                .playToServer(TakeRequestPayload.TYPE, TakeRequestPayload.STREAM_CODEC, TerminalNetwork::handleTake)
                .playToServer(InsertRequestPayload.TYPE, InsertRequestPayload.STREAM_CODEC,
                        TerminalNetwork::handleInsert)
                .playToClient(QueryResultPayload.TYPE, QueryResultPayload.STREAM_CODEC,
                        (payload, context) -> com.blackrikka.ironworks.client.ClientTerminalHandler
                                .handleQueryResult(payload, context));
    }

    private static void handleQuery(QueryRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            QueryPage page = TerminalActions.query(player, payload.query(), payload.sort(), payload.descending(),
                    Math.max(0, payload.page()));
            PacketDistributor.sendToPlayer(player, new QueryResultPayload(page));
        }
    }

    private static void handleTake(TakeRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            TerminalActions.Outcome outcome = TerminalActions.take(player, payload.template(), payload.mode());
            report(player, outcome);
        }
    }

    private static void handleInsert(InsertRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            TerminalActions.Outcome outcome = TerminalActions.insert(player, payload.mode());
            report(player, outcome);
        }
    }

    private static void report(ServerPlayer player, TerminalActions.Outcome outcome) {
        if (!outcome.success()) {
            player.displayClientMessage(outcome.message(), true);
        }
    }
}
