package com.skillgrow.core.infrastructure.adapter.in.rest.dto;

import com.skillgrow.core.domain.model.PageResult;
import java.util.List;
import java.util.function.Function;

/** Lista paginada del contrato: { totalRecords, page, pageSize, items }. */
public record PageResponse<T>(long totalRecords, int page, int pageSize, List<T> items) {

    public static <D, T> PageResponse<T> of(PageResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(result.totalRecords(), result.page(), result.pageSize(),
                result.items().stream().map(mapper).toList());
    }
}
