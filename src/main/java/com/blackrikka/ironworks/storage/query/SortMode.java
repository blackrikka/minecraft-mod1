package com.blackrikka.ironworks.storage.query;

/** Result order. Ties are always broken by registry id, ascending. */
public enum SortMode {
    /** By path, then namespace. */
    NAME,
    /** By stored count. */
    COUNT,
    /** By registry id (the numeric order of the item registry). */
    ID
}
