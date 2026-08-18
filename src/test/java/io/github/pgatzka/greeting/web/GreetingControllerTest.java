/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import io.github.pgatzka.greeting.config.ApiSecurityProperties;
import io.github.pgatzka.greeting.config.SecurityConfig;
import io.github.pgatzka.greeting.domain.Greeting;
import io.github.pgatzka.greeting.service.GreetingNotFoundException;
import io.github.pgatzka.greeting.service.GreetingService;
import io.github.pgatzka.greeting.service.UnsupportedLanguageException;

/**
 * Web-layer slice: routing, serialisation, validation, authorisation and error mapping, with the
 * service mocked out. The database is not involved.
 */
@WebMvcTest(GreetingController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties({ApiSecurityProperties.class, WebEndpointProperties.class})
class GreetingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GreetingService service;

  private static Greeting storedGreeting() {
    Greeting greeting = new Greeting("Ada", "de", "Hallo, Ada!");
    ReflectionTestUtils.setField(greeting, "id", 42L);
    ReflectionTestUtils.setField(greeting, "createdAt", Instant.parse("2026-08-18T09:15:30Z"));
    return greeting;
  }

  @Test
  @DisplayName("POST returns 201 with a Location header and the created body")
  void createsGreeting() throws Exception {
    when(service.createGreeting("Ada", "de")).thenReturn(storedGreeting());

    mockMvc
        .perform(
            post("/api/v1/greetings")
                .with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"Ada\",\"language\":\"de\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/v1/greetings/42"))
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.message").value("Hallo, Ada!"))
        .andExpect(jsonPath("$.createdAt").value("2026-08-18T09:15:30Z"));
  }

  @Test
  @DisplayName("POST without credentials is rejected before the service is called")
  void rejectsAnonymousCreate() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/greetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"Ada\"}"))
        .andExpect(status().isUnauthorized());

    verify(service, never()).createGreeting(any(), any());
  }

  @Test
  @DisplayName("POST with a blank recipient yields a problem detail listing the field")
  void rejectsInvalidPayload() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/greetings")
                .with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"  \",\"language\":\"toolong\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "language: language must be a two-letter code; "
                        + "recipient: recipient must not be blank"));
  }

  @Test
  @DisplayName("an unsupported language becomes a 400 problem detail naming the code")
  void mapsUnsupportedLanguage() throws Exception {
    when(service.createGreeting("Ada", "xx")).thenThrow(new UnsupportedLanguageException("xx"));

    mockMvc
        .perform(
            post("/api/v1/greetings")
                .with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipient\":\"Ada\",\"language\":\"xx\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Unsupported language"))
        .andExpect(jsonPath("$.requestedLanguage").value("xx"))
        .andExpect(jsonPath("$.type").value("urn:greeting-service:unsupported-language"));
  }

  @Test
  @DisplayName("GET by id is public")
  void readsGreetingAnonymously() throws Exception {
    when(service.getGreeting(42L)).thenReturn(storedGreeting());

    mockMvc
        .perform(get("/api/v1/greetings/42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipient").value("Ada"))
        .andExpect(jsonPath("$.language").value("de"));
  }

  @Test
  @DisplayName("a missing greeting becomes a 404 problem detail carrying the identifier")
  void mapsNotFound() throws Exception {
    when(service.getGreeting(99L)).thenThrow(new GreetingNotFoundException(99L));

    mockMvc
        .perform(get("/api/v1/greetings/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Greeting not found"))
        .andExpect(jsonPath("$.greetingId").value(99));
  }

  @Test
  @DisplayName("GET list applies the default page size and createdAt descending sort")
  void listsWithDefaultPageable() throws Exception {
    when(service.listGreetings(eq(null), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(storedGreeting()), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/v1/greetings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(42));

    verify(service)
        .listGreetings(
            null,
            PageRequest.of(
                0, 20, org.springframework.data.domain.Sort.by("createdAt").descending()));
  }

  @Test
  @DisplayName("GET list passes the language filter through")
  void listsWithLanguageFilter() throws Exception {
    when(service.listGreetings(eq("de"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(storedGreeting()), PageRequest.of(0, 5), 1));

    mockMvc
        .perform(get("/api/v1/greetings").param("language", "de").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].language").value("de"));
  }

  @Test
  @DisplayName("DELETE needs the admin role; a plain user gets 403")
  void forbidsDeleteForNonAdmin() throws Exception {
    mockMvc
        .perform(delete("/api/v1/greetings/42").with(httpBasic("user", "password")))
        .andExpect(status().isForbidden());

    verify(service, never()).deleteGreeting(42L);
  }

  @Test
  void deletesAsAdmin() throws Exception {
    mockMvc
        .perform(delete("/api/v1/greetings/42").with(httpBasic("admin", "password")))
        .andExpect(status().isNoContent());

    verify(service).deleteGreeting(42L);
  }

  @Test
  void mapsNotFoundOnDelete() throws Exception {
    doThrow(new GreetingNotFoundException(99L)).when(service).deleteGreeting(99L);

    mockMvc
        .perform(delete("/api/v1/greetings/99").with(httpBasic("admin", "password")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.greetingId").value(99));
  }
}
