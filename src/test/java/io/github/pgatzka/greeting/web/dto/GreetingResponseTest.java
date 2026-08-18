/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.pgatzka.greeting.domain.Greeting;

class GreetingResponseTest {

  @Test
  void copiesEveryFieldOffTheEntity() {
    Instant createdAt = Instant.parse("2026-08-18T09:15:30Z");
    Greeting greeting = new Greeting("Ada", "de", "Hallo, Ada!");
    ReflectionTestUtils.setField(greeting, "id", 42L);
    ReflectionTestUtils.setField(greeting, "createdAt", createdAt);

    GreetingResponse response = GreetingResponse.from(greeting);

    assertThat(response)
        .isEqualTo(new GreetingResponse(42L, "Ada", "de", "Hallo, Ada!", createdAt))
        .hasSameHashCodeAs(new GreetingResponse(42L, "Ada", "de", "Hallo, Ada!", createdAt));
    assertThat(response.toString()).contains("recipient=Ada");
  }
}
