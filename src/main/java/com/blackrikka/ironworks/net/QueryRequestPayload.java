package com.blackrikka.ironworks.net;

import com.blackrikka.ironworks.storage.query.SortMode;
import com.blackrikka.ironworks.storage.terminal.TerminalActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: asks for one page of the open terminal's search results.
 *
 * @param query the query text, at most {@link TerminalActions#MAX_QUERY_LENGTH} characters
 * @param sort the sort order
 * @param descending true for descending order
 * @param page the page index, 0-based
 */
public record QueryRequestPayload(String query, SortMode sort, boolean descending, int page)
        implements CustomPacketPayload {
    public static final Type<QueryRequestPayload> TYPE = new Type<>(TerminalNetwork.id("query_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QueryRequestPayload> STREAM_CODEC =
            StreamCodec.of(QueryRequestPayload::write, QueryRequestPayload::read);

    public QueryRequestPayload {
        query = query == null ? "" : query;
        sort = sort == null ? SortMode.NAME : sort;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, QueryRequestPayload payload) {
        String text = payload.query();
        if (text.length() > TerminalActions.MAX_QUERY_LENGTH) {
            text = text.substring(0, TerminalActions.MAX_QUERY_LENGTH);
        }
        buf.writeUtf(text, TerminalActions.MAX_QUERY_LENGTH);
        buf.writeEnum(payload.sort());
        buf.writeBoolean(payload.descending());
        buf.writeVarInt(payload.page());
    }

    private static QueryRequestPayload read(RegistryFriendlyByteBuf buf) {
        String text = buf.readUtf(TerminalActions.MAX_QUERY_LENGTH);
        SortMode sort = buf.readEnum(SortMode.class);
        boolean descending = buf.readBoolean();
        int page = buf.readVarInt();
        return new QueryRequestPayload(text, sort, descending, page);
    }
}
