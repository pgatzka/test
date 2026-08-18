/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageTest {

  @ParameterizedTest
  @CsvSource({
    "EN, Ada, 'Hello, Ada!'",
    "DE, Ada, 'Hallo, Ada!'",
    "FR, Ada, 'Bonjour, Ada !'",
    "ES, Ada, '¡Hola, Ada!'"
  })
  @DisplayName("renders the recipient into each language's template")
  void rendersTemplatePerLanguage(Language language, String name, String expected) {
    assertThat(language.render(name)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "DE", " de ", "De"})
  @DisplayName("resolves codes case-insensitively and ignores surrounding whitespace")
  void resolvesCodeLeniently(String code) {
    assertThat(Language.fromCode(code)).contains(Language.DE);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "xx", "english"})
  @DisplayName("returns empty for absent, blank or unknown codes")
  void returnsEmptyForUnresolvableCodes(String code) {
    assertThat(Language.fromCode(code)).isEqualTo(Optional.empty());
  }

  @Test
  void exposesEveryCodeInTheSupportedList() {
    assertThat(Language.supportedCodes()).isEqualTo("en, de, fr, es");
  }

  @Test
  void everyConstantExposesItsOwnCode() {
    assertThat(Language.values())
        .extracting(Language::code)
        .containsExactly("en", "de", "fr", "es");
  }
}
