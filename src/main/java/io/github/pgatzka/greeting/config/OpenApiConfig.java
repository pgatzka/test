/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/** Describes the API for springdoc, which serves it at /v3/api-docs and /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI greetingOpenApi(@Value("${spring.application.name}") String applicationName) {
    return new OpenAPI()
        .info(
            new Info()
                .title(applicationName)
                .version("v1")
                .description(
                    "Reference Spring Boot WebMVC service: renders greetings and stores them in "
                        + "PostgreSQL.")
                .license(new License().name("MIT")))
        .servers(List.of(new Server().url("/").description("Current host")))
        .components(
            new Components()
                .addSecuritySchemes(
                    "basicAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Demo accounts: user/password and admin/password")));
  }
}
