/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GreetingTest {

  @Test
  @DisplayName("two unsaved greetings are never equal, even with identical field values")
  void transientInstancesAreNotEqual() {
    Greeting first = new Greeting("Ada", "en", "Hello, Ada!");
    Greeting second = new Greeting("Ada", "en", "Hello, Ada!");

    assertThat(first).isNotEqualTo(second).isEqualTo(first);
  }

  @Test
  @DisplayName("saved greetings compare by identifier")
  void persistedInstancesCompareById() {
    Greeting first = new Greeting("Ada", "en", "Hello, Ada!");
    Greeting second = new Greeting("Grace", "de", "Hallo, Grace!");
    ReflectionTestUtils.setField(first, "id", 1L);
    ReflectionTestUtils.setField(second, "id", 1L);

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }

  @Test
  void isNotEqualToOtherTypesOrNull() {
    Greeting greeting = new Greeting("Ada", "en", "Hello, Ada!");

    assertThat(greeting).isNotEqualTo("Ada").isNotEqualTo(null);
  }

  @Test
  void toStringExposesIdentityWithoutTheMessageBody() {
    Greeting greeting = new Greeting("Ada", "en", "Hello, Ada!");
    ReflectionTestUtils.setField(greeting, "id", 3L);

    assertThat(greeting.toString())
        .contains("id=3", "recipient='Ada'", "languageCode='en'")
        .doesNotContain("Hello, Ada!");
  }

  @Test
  void hashCodeIsStableForUnsavedInstances() {
    assertThat(new Greeting("Ada", "en", "Hello, Ada!")).hasSameHashCodeAs(0);
  }
}
