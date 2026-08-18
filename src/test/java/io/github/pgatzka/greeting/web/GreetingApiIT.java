/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import io.github.pgatzka.greeting.domain.Greeting;
import io.github.pgatzka.greeting.support.AbstractPostgresIT;

/** End-to-end coverage of the API against a real PostgreSQL, through the real security filters. */
class GreetingApiIT extends AbstractPostgresIT {

  @Test
  @DisplayName("a created greeting is persisted and readable at its Location")
  void createsAndReadsBackAGreeting() {
    ResponseEntity<Map<String, Object>> created =
        restTemplate
            .withBasicAuth("user", "password")
            .exchange(
                createRequest("Ada", "de"),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).containsEntry("message", "Hallo, Ada!");

    URI location = created.getHeaders().getLocation();
    assertThat(location).isNotNull();

    ResponseEntity<Map<String, Object>> fetched =
        restTemplate.exchange(
            org.springframework.http.RequestEntity.get(location).build(),
            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody())
        .containsEntry("recipient", "Ada")
        .containsEntry("language", "de")
        .containsKey("createdAt");

    assertThat(greetingRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("createdAt is populated by JPA auditing on insert")
  void stampsCreatedAt() {
    Greeting saved = greetingRepository.save(new Greeting("Grace", "en", "Hello, Grace!"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("writing without credentials is rejected and nothing is stored")
  void rejectsAnonymousWrites() {
    ResponseEntity<String> response =
        restTemplate.exchange(createRequest("Ada", "en"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(greetingRepository.count()).isZero();
  }

  @Test
  @DisplayName("an unsupported language is refused with a problem detail")
  void refusesUnsupportedLanguage() {
    ResponseEntity<ProblemDetail> response =
        restTemplate
            .withBasicAuth("user", "password")
            .exchange(createRequest("Ada", "xx"), ProblemDetail.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).contains("Unsupported language 'xx'");
    assertThat(greetingRepository.count()).isZero();
  }

  @Test
  @DisplayName("listing filters by language and reports pagination metadata")
  void listsAndFilters() {
    greetingRepository.save(new Greeting("Ada", "en", "Hello, Ada!"));
    greetingRepository.save(new Greeting("Grace", "de", "Hallo, Grace!"));
    greetingRepository.save(new Greeting("Alan", "de", "Hallo, Alan!"));

    ResponseEntity<Map<String, Object>> all =
        restTemplate.exchange(
            org.springframework.http.RequestEntity.get("/api/v1/greetings").build(),
            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(all.getBody()).containsEntry("totalElements", 3);

    ResponseEntity<Map<String, Object>> german =
        restTemplate.exchange(
            org.springframework.http.RequestEntity.get("/api/v1/greetings?language=de&size=1")
                .build(),
            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(german.getBody())
        .containsEntry("totalElements", 2)
        .containsEntry("totalPages", 2)
        .containsEntry("size", 1);
  }

  @Test
  @DisplayName("only an admin may delete, and the row really disappears")
  void deletesAsAdminOnly() {
    Greeting stored = greetingRepository.save(new Greeting("Ada", "en", "Hello, Ada!"));
    String path = "/api/v1/greetings/" + stored.getId();

    assertThat(
            restTemplate
                .withBasicAuth("user", "password")
                .exchange(org.springframework.http.RequestEntity.delete(path).build(), String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(greetingRepository.count()).isEqualTo(1);

    assertThat(
            restTemplate
                .withBasicAuth("admin", "password")
                .exchange(org.springframework.http.RequestEntity.delete(path).build(), String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(greetingRepository.count()).isZero();
  }

  @Test
  @DisplayName("deleting something that is not there is a 404")
  void deleteOfMissingGreetingIsNotFound() {
    ResponseEntity<ProblemDetail> response =
        restTemplate
            .withBasicAuth("admin", "password")
            .exchange(
                org.springframework.http.RequestEntity.delete("/api/v1/greetings/999999").build(),
                ProblemDetail.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private org.springframework.http.RequestEntity<Map<String, String>> createRequest(
      String recipient, String language) {
    return org.springframework.http.RequestEntity.post("/api/v1/greetings")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(Map.of("recipient", recipient, "language", language));
  }
}
