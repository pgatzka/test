/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web.dto;

import java.time.Instant;

import io.github.pgatzka.greeting.domain.Greeting;
import io.swagger.v3.oas.annotations.media.Schema;

/** API representation of a stored greeting. */
@Schema(description = "A greeting that was rendered and stored")
public record GreetingResponse(
    @Schema(example = "42") Long id,
    @Schema(example = "Ada") String recipient,
    @Schema(example = "de") String language,
    @Schema(example = "Hallo, Ada!") String message,
    @Schema(example = "2026-08-18T09:15:30Z") Instant createdAt) {

  /** Maps the entity onto the wire format, keeping persistence details out of the API. */
  public static GreetingResponse from(Greeting greeting) {
    return new GreetingResponse(
        greeting.getId(),
        greeting.getRecipient(),
        greeting.getLanguageCode(),
        greeting.getMessage(),
        greeting.getCreatedAt());
  }
}
