package com.blackrikka.ironworks.storage.query;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * One stored item kind as seen by the query engine: registry id parts, tags and the total stored count.
 * Plain data without Minecraft classes, so {@link DbQuery} can be unit tested. Namespace, path and tags are
 * lowercased on construction.
 *
 * @param namespace the mod id, for example {@code minecraft}
 * @param path the item path, for example {@code iron_ore}
 * @param tags item tags in full form, for example {@code minecraft:logs}
 * @param count the stored amount, zero or more
 */
public record QueryRow(String namespace, String path, Set<String> tags, long count) {
    public QueryRow {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        namespace = namespace.toLowerCase(Locale.ROOT);
        path = path.toLowerCase(Locale.ROOT);
        tags = Set.copyOf(tags.stream().map(tag -> tag.toLowerCase(Locale.ROOT)).toList());
    }

    /** The full registry id, for example {@code minecraft:iron_ore}. */
    public String id() {
        return namespace + ":" + path;
    }
}
