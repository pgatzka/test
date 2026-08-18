/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.support;

import org.springframework.boot.SpringApplication;

import io.github.pgatzka.greeting.GreetingApplication;

/**
 * Local development entry point: {@code mvn spring-boot:test-run} boots the app against a
 * Testcontainers PostgreSQL, so no database has to be installed or started by hand.
 */
public final class TestGreetingApplication {

  private TestGreetingApplication() {}

  public static void main(String[] args) {
    SpringApplication.from(GreetingApplication::main)
        .with(PostgresTestcontainerConfiguration.class)
        .run(args);
  }
}
