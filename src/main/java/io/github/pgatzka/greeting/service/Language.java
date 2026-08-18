/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Languages the service can greet in.
 *
 * <p>Each constant owns its own message template, so adding a language is a one-line change and
 * never touches {@link GreetingService}.
 */
public enum Language {
  EN("en", "Hello, %s!"),
  DE("de", "Hallo, %s!"),
  FR("fr", "Bonjour, %s !"),
  ES("es", "¡Hola, %s!");

  private final String code;
  private final String template;

  Language(String code, String template) {
    this.code = code;
    this.template = template;
  }

  public String code() {
    return code;
  }

  /** Renders the greeting for {@code name} in this language. */
  public String render(String name) {
    return String.format(template, name);
  }

  /**
   * Resolves an ISO 639-1 code, case-insensitively.
   *
   * @param code the language code, may be {@code null} or blank
   * @return the matching language, or empty when the code is unknown or absent
   */
  public static Optional<Language> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    String normalised = code.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(language -> language.code.equals(normalised)).findFirst();
  }

  /** Comma-separated list of supported codes, for error messages and API documentation. */
  public static String supportedCodes() {
    return String.join(", ", Arrays.stream(values()).map(Language::code).toList());
  }
}
