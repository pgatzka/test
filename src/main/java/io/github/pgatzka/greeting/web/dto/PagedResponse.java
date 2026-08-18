/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A stable page envelope.
 *
 * <p>Serialising Spring's {@code Page} directly leaks its internal structure into the API and its
 * shape changes between Spring Data versions, so we map to our own record instead.
 */
@Schema(description = "One page of results")
public record PagedResponse<T>(
    List<T> content,
    @Schema(example = "0") int page,
    @Schema(example = "20") int size,
    @Schema(example = "137") long totalElements,
    @Schema(example = "7") int totalPages) {

  public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
    return new PagedResponse<>(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
