/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import io.github.pgatzka.greeting.domain.Greeting;

class PagedResponseTest {

  @Test
  @DisplayName("copies pagination metadata and maps every element")
  void mapsPageMetadataAndContent() {
    PageImpl<Greeting> page =
        new PageImpl<>(List.of(new Greeting("Ada", "en", "Hello, Ada!")), PageRequest.of(1, 5), 11);

    PagedResponse<GreetingResponse> response = PagedResponse.from(page, GreetingResponse::from);

    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(5);
    assertThat(response.totalElements()).isEqualTo(11);
    assertThat(response.totalPages()).isEqualTo(3);
    assertThat(response.content())
        .singleElement()
        .extracting(GreetingResponse::message)
        .isEqualTo("Hello, Ada!");
  }

  @Test
  void handlesAnEmptyPage() {
    PagedResponse<GreetingResponse> response =
        PagedResponse.from(new PageImpl<Greeting>(List.of()), GreetingResponse::from);

    assertThat(response.content()).isEmpty();
    assertThat(response.totalElements()).isZero();
  }

  @Test
  void recordsCompareByValue() {
    PagedResponse<String> first = new PagedResponse<>(List.of("a"), 0, 1, 1, 1);
    PagedResponse<String> second = new PagedResponse<>(List.of("a"), 0, 1, 1, 1);

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    assertThat(first.toString()).contains("totalElements=1");
  }
}
