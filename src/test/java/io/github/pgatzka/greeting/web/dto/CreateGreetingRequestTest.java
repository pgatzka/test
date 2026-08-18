/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CreateGreetingRequestTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void acceptsAValidPayload() {
    assertThat(validate(new CreateGreetingRequest("Ada", "de"))).isEmpty();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  @DisplayName("rejects a blank recipient")
  void rejectsBlankRecipient(String recipient) {
    assertThat(validate(new CreateGreetingRequest(recipient, "en")))
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("recipient"));
  }

  @Test
  void rejectsAnOverlongRecipient() {
    assertThat(validate(new CreateGreetingRequest("A".repeat(101), "en")))
        .singleElement()
        .extracting(ConstraintViolation::getMessage)
        .isEqualTo("recipient must be at most 100 characters");
  }

  @ParameterizedTest
  @ValueSource(strings = {"e", "eng", "d1"})
  void rejectsMalformedLanguageCodes(String language) {
    assertThat(validate(new CreateGreetingRequest("Ada", language)))
        .singleElement()
        .extracting(ConstraintViolation::getMessage)
        .isEqualTo("language must be a two-letter code");
  }

  @ParameterizedTest
  @NullSource
  @DisplayName("an absent language is valid; the service defaults it to English")
  void allowsAnAbsentLanguage(String language) {
    assertThat(validate(new CreateGreetingRequest("Ada", language))).isEmpty();
  }

  private Set<ConstraintViolation<CreateGreetingRequest>> validate(CreateGreetingRequest request) {
    return validator.validate(request);
  }
}
