package dev.infreej.jeckathon.global.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

// 목록 응답 형식: { "items": [...], "page": 0, "size": 20, "totalCount": 135 }
public record PageResponse<T>(List<T> items, int page, int size, long totalCount) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
	}

	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		return from(page.map(mapper));
	}
}
