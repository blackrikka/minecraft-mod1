package com.blackrikka.ironworks.storage.query;

/**
 * A query from a client or a command.
 *
 * @param query the query text, see {@link DbQuery}; {@code null} is treated as empty (all kinds)
 * @param sort the sort order
 * @param descending true for descending order
 * @param page the page index, starting at 0; out-of-range values are corrected by {@link QueryService}
 * @param pageSize the number of entries per page, at least 1
 */
public record QueryRequest(String query, SortMode sort, boolean descending, int page, int pageSize) {
    public QueryRequest {
        query = query == null ? "" : query;
        sort = sort == null ? SortMode.NAME : sort;
    }
}
