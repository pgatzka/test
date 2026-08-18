/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.pgatzka.greeting.domain.Greeting;
import io.github.pgatzka.greeting.service.GreetingService;
import io.github.pgatzka.greeting.web.dto.CreateGreetingRequest;
import io.github.pgatzka.greeting.web.dto.GreetingResponse;
import io.github.pgatzka.greeting.web.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HTTP API for rendering and retrieving greetings. */
@RestController
@RequestMapping("/api/v1/greetings")
@Tag(name = "Greetings", description = "Render, retrieve and delete greetings")
public class GreetingController {

  private final GreetingService service;

  public GreetingController(GreetingService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Render and store a greeting")
  @SecurityRequirement(name = "basicAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Greeting created"),
    @ApiResponse(responseCode = "400", description = "Invalid payload or unsupported language"),
    @ApiResponse(responseCode = "401", description = "Missing or bad credentials")
  })
  public ResponseEntity<GreetingResponse> create(
      @Valid @RequestBody CreateGreetingRequest request, UriComponentsBuilder uriBuilder) {
    Greeting greeting = service.createGreeting(request.recipient(), request.language());
    URI location =
        uriBuilder.path("/api/v1/greetings/{id}").buildAndExpand(greeting.getId()).toUri();
    return ResponseEntity.created(location).body(GreetingResponse.from(greeting));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Fetch one greeting by identifier")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Greeting found"),
    @ApiResponse(responseCode = "404", description = "No greeting with that identifier")
  })
  public GreetingResponse getById(@PathVariable long id) {
    return GreetingResponse.from(service.getGreeting(id));
  }

  @GetMapping
  @Operation(summary = "List stored greetings, newest first")
  public PagedResponse<GreetingResponse> list(
      @Parameter(description = "Optional ISO 639-1 filter, for example 'de'")
          @RequestParam(required = false)
          String language,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<Greeting> greetings = service.listGreetings(language, pageable);
    return PagedResponse.from(greetings, GreetingResponse::from);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a greeting")
  @SecurityRequirement(name = "basicAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Greeting deleted"),
    @ApiResponse(responseCode = "403", description = "Authenticated but not an admin"),
    @ApiResponse(responseCode = "404", description = "No greeting with that identifier")
  })
  public void delete(@PathVariable long id) {
    service.deleteGreeting(id);
  }
}
