/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credentials for the two demo accounts.
 *
 * <p>These are in-memory accounts for an example service. A real deployment would authenticate
 * against an identity provider; the passwords here exist so the API is not open by default and so
 * the integration tests have something to log in with.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record ApiSecurityProperties(@Valid Account user, @Valid Account admin) {

  public ApiSecurityProperties {
    user = user == null ? new Account("user", "password") : user;
    admin = admin == null ? new Account("admin", "password") : admin;
  }

  /** A username and its plaintext password, encoded with BCrypt at startup. */
  public record Account(@NotBlank String username, @NotBlank String password) {}
}
