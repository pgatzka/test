/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web;

import java.net.URI;
import java.util.Comparator;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.github.pgatzka.greeting.service.GreetingNotFoundException;
import io.github.pgatzka.greeting.service.UnsupportedLanguageException;

/**
 * Translates exceptions into RFC 9457 problem details, so every error response has the same shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final URI NOT_FOUND_TYPE = URI.create("urn:greeting-service:greeting-not-found");
  private static final URI UNSUPPORTED_LANGUAGE_TYPE =
      URI.create("urn:greeting-service:unsupported-language");
  private static final URI VALIDATION_TYPE = URI.create("urn:greeting-service:validation-failed");

  @ExceptionHandler(GreetingNotFoundException.class)
  public ProblemDetail handleNotFound(GreetingNotFoundException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Greeting not found");
    problem.setType(NOT_FOUND_TYPE);
    problem.setProperty("greetingId", exception.getId());
    return problem;
  }

  @ExceptionHandler(UnsupportedLanguageException.class)
  public ProblemDetail handleUnsupportedLanguage(UnsupportedLanguageException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Unsupported language");
    problem.setType(UNSUPPORTED_LANGUAGE_TYPE);
    problem.setProperty("requestedLanguage", exception.getRequestedCode());
    return problem;
  }

  /**
   * Turns Bean Validation failures into a single, deterministically ordered detail message rather
   * than the default framework text.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String detail =
        exception.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(error -> error.getField()))
            .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
            .reduce((left, right) -> left + "; " + right)
            .orElse("Request validation failed.");

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Request validation failed");
    problem.setType(VALIDATION_TYPE);
    return ResponseEntity.badRequest().body(problem);
  }
}
