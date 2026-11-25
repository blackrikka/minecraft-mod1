package com.blackrikka.ironworks.storage.query;

import com.blackrikka.ironworks.storage.ItemKey;
import com.blackrikka.ironworks.storage.StorageNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server-side query, extract and insert over a {@link StorageNetwork}. Server only.
 * <p>
 * A query reads the network snapshot, turns each kind into a {@link QueryRow}, applies the {@link DbQuery} filter,
 * sorts (ties by registry id) and returns one page. Nothing is stored between queries.
 */
public final class QueryService {
    /** Entries per page: the 9 x 5 result grid of the terminal. */
    public static final int PAGE_SIZE = 45;

    private QueryService() {
    }

    /** Runs {@code request} against {@code network}. An offline network gives an empty page with {@code online=false}. */
    public static QueryPage run(StorageNetwork network, QueryRequest request) {
        DbQuery.ParseResult parsed = DbQuery.parse(request.query());
        int rackCount = network.providers().size();
        if (!network.isOnline()) {
            return new QueryPage(List.of(), 0, 0, 1, false, 0, rackCount, parsed.errors());
        }

        List<Candidate> candidates = new ArrayList<>();
        long usedItems = 0;
        for (Map.Entry<ItemKey, Long> entry : network.snapshot().entrySet()) {
            long count = entry.getValue();
            usedItems += count;
            ItemStack template = entry.getKey().stack();
            QueryRow row = toRow(template, count);
            if (count > 0 && parsed.filter().test(row)) {
                candidates.add(new Candidate(template, count, row, BuiltInRegistries.ITEM.getId(template.getItem())));
            }
        }

        candidates.sort(comparator(request.sort(), request.descending()));

        int pageSize = Math.max(1, request.pageSize());
        int total = candidates.size();
        int pageCount = Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(Math.max(request.page(), 0), pageCount - 1);
        int from = page * pageSize;
        int to = Math.min(total, from + pageSize);
        List<QueryPage.Entry> entries = candidates.subList(from, to).stream()
                .map(candidate -> new QueryPage.Entry(candidate.template(), candidate.count()))
                .collect(Collectors.toList());
        return new QueryPage(entries, total, page, pageCount, true, usedItems, rackCount, parsed.errors());
    }

    /**
     * Takes up to {@code amount} of {@code template}'s kind from the network.
     *
     * @return the amount removed
     */
    public static long extract(StorageNetwork network, ItemStack template, long amount) {
        return network.extract(template, amount, false);
    }

    /**
     * Puts up to {@code amount} of {@code template}'s kind into the network.
     *
     * @return the amount accepted
     */
    public static long insert(StorageNetwork network, ItemStack template, long amount) {
        return network.insert(template, amount, false);
    }

    private static QueryRow toRow(ItemStack template, long count) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(template.getItem());
        Set<String> tags = template.getTags()
                .map(tag -> tag.location().toString())
                .collect(Collectors.toSet());
        return new QueryRow(id.getNamespace(), id.getPath(), tags, count);
    }

    private static Comparator<Candidate> comparator(SortMode sort, boolean descending) {
        Comparator<Candidate> primary = switch (sort) {
            case NAME -> Comparator.comparing((Candidate c) -> c.row().path())
                    .thenComparing(c -> c.row().namespace());
            case COUNT -> Comparator.comparingLong(Candidate::count);
            case ID -> Comparator.comparingInt(Candidate::registryId);
        };
        if (descending) {
            primary = primary.reversed();
        }
        return primary.thenComparingInt(Candidate::registryId);
    }

    /** A matching kind with the data the sort needs. */
    private record Candidate(ItemStack template, long count, QueryRow row, int registryId) {
    }
}
