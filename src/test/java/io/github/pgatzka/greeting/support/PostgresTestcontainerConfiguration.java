/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway PostgreSQL for tests.
 *
 * <p>{@code @ServiceConnection} wires the container's JDBC URL and credentials into the context, so
 * nothing has to be configured in properties. The same class backs {@code mvn spring-boot:test-run}
 * for local development against a real database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainerConfiguration {

  /** Pinned by digest-free tag so CI is reproducible; bump deliberately. */
  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(POSTGRES_IMAGE)
        .withDatabaseName("greeting")
        .withUsername("greeting")
        .withPassword("greeting")
        .withReuse(true);
  }
}
