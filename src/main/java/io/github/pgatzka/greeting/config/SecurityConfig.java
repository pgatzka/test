/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic security for a stateless API.
 *
 * <p>Reads are public so the example is easy to explore; writes need an account and deletes need
 * the admin role.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(
      ApiSecurityProperties properties, PasswordEncoder passwordEncoder) {
    UserDetails user =
        User.withUsername(properties.user().username())
            .password(passwordEncoder.encode(properties.user().password()))
            .roles("USER")
            .build();
    UserDetails admin =
        User.withUsername(properties.admin().username())
            .password(passwordEncoder.encode(properties.admin().password()))
            .roles("USER", "ADMIN")
            .build();
    return new InMemoryUserDetailsManager(user, admin);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, WebEndpointProperties endpointProperties) throws Exception {
    String actuatorBase = endpointProperties.getBasePath();

    return http.csrf(csrf -> csrf.disable())
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            registry ->
                registry
                    .requestMatchers(actuatorBase + "/health", actuatorBase + "/health/**")
                    .permitAll()
                    .requestMatchers(actuatorBase + "/info")
                    .permitAll()
                    .requestMatchers(actuatorBase + "/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/greetings/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/greetings/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .build();
  }
}
