package com.itmonteur.hospitalerp.common;

import java.util.List;

/** One page of a list as the API returns it; {@code page} counts from 0. */
public record PageResult<T>(List<T> entries, int page, int size, long totalEntries, int totalPages) {

    public static <T> PageResult<T> of(List<T> entries, int page, int size, long totalEntries) {
        return new PageResult<>(entries, page, size, totalEntries, (int) ((totalEntries + size - 1) / size));
    }
}
