/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of {@code POST /api/v1/greetings}.
 *
 * <p>Bean Validation rejects malformed input before it ever reaches the service, which keeps the
 * service free of defensive checks it cannot act on.
 */
@Schema(description = "Request to render and store a greeting")
public record CreateGreetingRequest(
    @Schema(description = "Who to greet", example = "Ada")
        @NotBlank(message = "recipient must not be blank") @Size(max = 100, message = "recipient must be at most 100 characters") String recipient,
    @Schema(
            description = "ISO 639-1 language code; omit for English",
            example = "de",
            nullable = true)
        @Pattern(regexp = "^[a-zA-Z]{2}$", message = "language must be a two-letter code") String language) {}
