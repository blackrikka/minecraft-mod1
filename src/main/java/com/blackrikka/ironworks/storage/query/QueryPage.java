package com.blackrikka.ironworks.storage.query;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One page of query results.
 *
 * @param entries the kinds on this page; each template is a single item with the kind's components
 * @param totalMatches the number of kinds that match the query (all pages)
 * @param page the page shown, after correction to the valid range (0-based)
 * @param pageCount the number of pages, at least 1
 * @param online false if the network has no online provider; the result is then empty
 * @param usedItems the total amount stored in the whole network, not only the matching kinds
 * @param rackCount the number of server racks in the network
 * @param errors the query errors, see {@link DbQuery.ParseResult#errors()}
 */
public record QueryPage(List<Entry> entries, int totalMatches, int page, int pageCount, boolean online,
                        long usedItems, int rackCount, List<String> errors) {
    public QueryPage {
        entries = List.copyOf(entries);
        errors = List.copyOf(errors);
    }

    /** One kind on the page. */
    public record Entry(ItemStack template, long count) {
    }
}
