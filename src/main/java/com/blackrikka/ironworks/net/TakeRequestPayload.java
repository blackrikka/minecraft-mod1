package com.blackrikka.ironworks.net;

import com.blackrikka.ironworks.storage.terminal.TerminalActions.TakeMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: takes one kind out of the network, as the result slot was clicked.
 *
 * @param template the kind to take (a single item with the kind's components)
 * @param mode how much to take and where it goes
 */
public record TakeRequestPayload(ItemStack template, TakeMode mode) implements CustomPacketPayload {
    public static final Type<TakeRequestPayload> TYPE = new Type<>(TerminalNetwork.id("take_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TakeRequestPayload> STREAM_CODEC =
            StreamCodec.of(TakeRequestPayload::write, TakeRequestPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, TakeRequestPayload payload) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.template());
        buf.writeEnum(payload.mode());
    }

    private static TakeRequestPayload read(RegistryFriendlyByteBuf buf) {
        ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        TakeMode mode = buf.readEnum(TakeMode.class);
        return new TakeRequestPayload(template, mode);
    }
}
