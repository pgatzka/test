/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Checks the operational surface: migrations applied, health public, API docs served. */
class OperationsIT extends AbstractPostgresIT {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("both Flyway migrations ran successfully")
  void flywayAppliedEveryMigration() {
    List<Map<String, Object>> applied =
        jdbcTemplate.queryForList(
            "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank");

    assertThat(applied)
        .hasSize(2)
        .allSatisfy(row -> assertThat(row).containsEntry("success", true));
    assertThat(applied).extracting(row -> row.get("version")).containsExactly("1", "2");
  }

  @Test
  @DisplayName("the CHECK constraint from V2 rejects a blank recipient at the database level")
  void databaseRejectsBlankRecipient() {
    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    jdbcTemplate.update(
                        "INSERT INTO greeting (recipient, language_code, message, created_at)"
                            + " VALUES (' ', 'en', 'Hello!', now())")))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("the health endpoint is reachable without credentials and reports UP")
  void healthIsPublic() {
    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            org.springframework.http.RequestEntity.get("/actuator/health").build(),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "UP");
  }

  @Test
  @DisplayName("other actuator endpoints require the admin role")
  void metricsRequireAdmin() {
    assertThat(restTemplate.getForEntity("/actuator/metrics", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            restTemplate
                .withBasicAuth("admin", "password")
                .getForEntity("/actuator/metrics", String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("the OpenAPI document is served and describes the greetings API")
  void openApiDocumentIsPublic() {
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("/api/v1/greetings")
        .contains("basicAuth")
        .contains("greeting-service");
  }
}
