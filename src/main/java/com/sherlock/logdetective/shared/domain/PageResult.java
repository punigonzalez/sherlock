package com.sherlock.logdetective.shared.domain;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public PageResult {
		Objects.requireNonNull(items, "items");
		items = List.copyOf(items);
		if (page < 0) {
			throw new IllegalArgumentException("page must be >= 0");
		}
		if (size < 1) {
			throw new IllegalArgumentException("size must be >= 1");
		}
		if (totalElements < 0) {
			throw new IllegalArgumentException("totalElements must be >= 0");
		}
	}

	public static <T> PageResult<T> of(List<T> items, int page, int size, long totalElements) {
		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
		return new PageResult<>(items, page, size, totalElements, totalPages);
	}
}
