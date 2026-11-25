package com.blackrikka.ironworks.net;

import com.blackrikka.ironworks.storage.terminal.TerminalActions.InsertMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: puts the cursor stack of the open terminal into the network. The stack itself stays on the
 * server (it is the menu's carried item), so only the mode is sent.
 *
 * @param mode all of the cursor stack, or one item
 */
public record InsertRequestPayload(InsertMode mode) implements CustomPacketPayload {
    public static final Type<InsertRequestPayload> TYPE = new Type<>(TerminalNetwork.id("insert_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InsertRequestPayload> STREAM_CODEC =
            StreamCodec.of(InsertRequestPayload::write, InsertRequestPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, InsertRequestPayload payload) {
        buf.writeEnum(payload.mode());
    }

    private static InsertRequestPayload read(RegistryFriendlyByteBuf buf) {
        return new InsertRequestPayload(buf.readEnum(InsertMode.class));
    }
}
