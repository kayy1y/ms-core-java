package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Conversión entre la paginación del dominio (page desde 1) y la de Spring Data (desde 0). */
final class Pages {

    private Pages() {}

    static Pageable of(PageQuery q, Sort sort) {
        return PageRequest.of(q.page() - 1, q.pageSize(), sort);
    }

    static <E, T> PageResult<T> toResult(Page<E> page, PageQuery q, Function<E, T> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                q.page(), q.pageSize());
    }
}
