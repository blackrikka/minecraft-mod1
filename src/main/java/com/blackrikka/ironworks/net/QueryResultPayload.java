package com.blackrikka.ironworks.net;

import com.blackrikka.ironworks.storage.query.QueryPage;
import com.blackrikka.ironworks.storage.query.QueryService;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: one page of search results, the same content as {@link QueryPage}.
 *
 * @param page the page
 */
public record QueryResultPayload(QueryPage page) implements CustomPacketPayload {
    public static final Type<QueryResultPayload> TYPE = new Type<>(TerminalNetwork.id("query_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QueryResultPayload> STREAM_CODEC =
            StreamCodec.of(QueryResultPayload::write, QueryResultPayload::read);

    /** Upper bound on decoded errors; the server sends at most this many. */
    private static final int MAX_ERRORS = 64;
    private static final int MAX_ERROR_LENGTH = 512;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, QueryResultPayload payload) {
        QueryPage page = payload.page();
        buf.writeVarInt(page.totalMatches());
        buf.writeVarInt(page.page());
        buf.writeVarInt(page.pageCount());
        buf.writeBoolean(page.online());
        buf.writeVarLong(page.usedItems());
        buf.writeVarInt(page.rackCount());

        buf.writeVarInt(page.entries().size());
        for (QueryPage.Entry entry : page.entries()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.template());
            buf.writeVarLong(entry.count());
        }

        int errors = Math.min(page.errors().size(), MAX_ERRORS);
        buf.writeVarInt(errors);
        for (int i = 0; i < errors; i++) {
            String text = page.errors().get(i);
            buf.writeUtf(text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text,
                    MAX_ERROR_LENGTH);
        }
    }

    private static QueryResultPayload read(RegistryFriendlyByteBuf buf) {
        int totalMatches = buf.readVarInt();
        int page = buf.readVarInt();
        int pageCount = buf.readVarInt();
        boolean online = buf.readBoolean();
        long usedItems = buf.readVarLong();
        int rackCount = buf.readVarInt();

        int entryCount = buf.readVarInt();
        if (entryCount < 0 || entryCount > QueryService.PAGE_SIZE) {
            throw new DecoderException("terminal result has too many entries: " + entryCount);
        }
        List<QueryPage.Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long count = buf.readVarLong();
            entries.add(new QueryPage.Entry(template, count));
        }

        int errorCount = buf.readVarInt();
        if (errorCount < 0 || errorCount > MAX_ERRORS) {
            throw new DecoderException("terminal result has too many errors: " + errorCount);
        }
        List<String> errors = new ArrayList<>(errorCount);
        for (int i = 0; i < errorCount; i++) {
            errors.add(buf.readUtf(MAX_ERROR_LENGTH));
        }
        return new QueryResultPayload(new QueryPage(entries, totalMatches, page, pageCount, online, usedItems,
                rackCount, errors));
    }
}
