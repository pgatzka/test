/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables the auditing listener that populates {@code createdAt} on persist. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
