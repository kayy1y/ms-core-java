package com.skillgrow.core.domain.model;

/** Página pedida por el cliente: page desde 1, pageSize entre 1 y 50. */
public record PageQuery(int page, int pageSize) {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    public PageQuery {
        page = Math.max(page, 1);
        pageSize = pageSize <= 0 ? DEFAULT_SIZE : Math.min(pageSize, MAX_SIZE);
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
