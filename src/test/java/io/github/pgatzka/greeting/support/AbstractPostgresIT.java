/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;

import io.github.pgatzka.greeting.domain.GreetingRepository;

/**
 * Base class for full-stack integration tests: real Tomcat, real Spring Security, real Flyway-
 * migrated PostgreSQL.
 *
 * <p>The container is started once and shared by every subclass, because Spring's context cache
 * keeps the same application context alive across them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainerConfiguration.class)
public abstract class AbstractPostgresIT {

  @Autowired protected TestRestTemplate restTemplate;

  @Autowired protected GreetingRepository greetingRepository;

  /** Each test starts from an empty table, so ordering between tests cannot matter. */
  @BeforeEach
  void clearGreetings() {
    greetingRepository.deleteAll();
  }
}
