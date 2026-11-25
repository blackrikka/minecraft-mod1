package com.blackrikka.ironworks.client;

import com.blackrikka.ironworks.net.QueryResultPayload;
import com.blackrikka.ironworks.storage.terminal.DbTerminalScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side handling of terminal packets. Only referenced from a lambda in
 * {@link com.blackrikka.ironworks.net.TerminalNetwork}, so it is never loaded on a dedicated server.
 */
public final class ClientTerminalHandler {
    private ClientTerminalHandler() {
    }

    /** Passes the page to the terminal screen if that screen is open. */
    public static void handleQueryResult(QueryResultPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof DbTerminalScreen screen) {
            screen.acceptResult(payload.page());
        }
    }
}
