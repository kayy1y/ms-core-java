package com.skillgrow.core.domain.model;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado: { totalRecords, page, pageSize, items }. */
public record PageResult<T>(List<T> items, long totalRecords, int page, int pageSize) {

    public <R> PageResult<R> map(Function<T, R> fn) {
        return new PageResult<>(items.stream().map(fn).toList(), totalRecords, page, pageSize);
    }

    public static <T> PageResult<T> of(List<T> all, PageQuery q) {
        int from = Math.min(q.offset(), all.size());
        int to = Math.min(from + q.pageSize(), all.size());
        return new PageResult<>(all.subList(from, to), all.size(), q.page(), q.pageSize());
    }
}
