/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import io.github.pgatzka.greeting.domain.Greeting;
import io.github.pgatzka.greeting.domain.GreetingRepository;

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

  private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

  @Mock private GreetingRepository repository;

  @InjectMocks private GreetingService service;

  @Nested
  class CreateGreeting {

    @Test
    @DisplayName("renders in the requested language and persists the result")
    void persistsRenderedGreeting() {
      when(repository.save(any(Greeting.class))).thenAnswer(call -> call.getArgument(0));

      Greeting created = service.createGreeting("Ada", "de");

      ArgumentCaptor<Greeting> saved = ArgumentCaptor.forClass(Greeting.class);
      verify(repository).save(saved.capture());
      assertThat(saved.getValue().getMessage()).isEqualTo("Hallo, Ada!");
      assertThat(created.getRecipient()).isEqualTo("Ada");
      assertThat(created.getLanguageCode()).isEqualTo("de");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("falls back to English when no language is given")
    void defaultsToEnglish(String language) {
      when(repository.save(any(Greeting.class))).thenAnswer(call -> call.getArgument(0));

      Greeting created = service.createGreeting("Ada", language);

      assertThat(created.getLanguageCode()).isEqualTo("en");
      assertThat(created.getMessage()).isEqualTo("Hello, Ada!");
    }

    @Test
    @DisplayName("trims the recipient before rendering and storing")
    void trimsRecipient() {
      when(repository.save(any(Greeting.class))).thenAnswer(call -> call.getArgument(0));

      Greeting created = service.createGreeting("  Ada  ", "en");

      assertThat(created.getRecipient()).isEqualTo("Ada");
      assertThat(created.getMessage()).isEqualTo("Hello, Ada!");
    }

    @Test
    @DisplayName("rejects an unknown language without touching the database")
    void rejectsUnknownLanguage() {
      assertThatThrownBy(() -> service.createGreeting("Ada", "xx"))
          .isInstanceOf(UnsupportedLanguageException.class)
          .hasMessageContaining("xx")
          .hasMessageContaining("en, de, fr, es");

      verify(repository, never()).save(any());
    }
  }

  @Nested
  class GetGreeting {

    @Test
    void returnsTheStoredGreeting() {
      Greeting stored = new Greeting("Ada", "en", "Hello, Ada!");
      when(repository.findById(7L)).thenReturn(Optional.of(stored));

      assertThat(service.getGreeting(7L)).isSameAs(stored);
    }

    @Test
    void throwsWhenAbsent() {
      when(repository.findById(7L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getGreeting(7L))
          .isInstanceOf(GreetingNotFoundException.class)
          .hasMessageContaining("7");
    }
  }

  @Nested
  class ListGreetings {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("lists every language when no filter is supplied")
    void listsAllWithoutFilter(String filter) {
      Page<Greeting> page = new PageImpl<>(List.of(new Greeting("Ada", "en", "Hello, Ada!")));
      when(repository.findAll(FIRST_PAGE)).thenReturn(page);

      assertThat(service.listGreetings(filter, FIRST_PAGE)).isSameAs(page);
      verify(repository, never()).findByLanguageCode(any(), any());
    }

    @Test
    @DisplayName("normalises the filter before querying")
    void normalisesFilter() {
      Page<Greeting> page = new PageImpl<>(List.of(new Greeting("Ada", "de", "Hallo, Ada!")));
      when(repository.findByLanguageCode(eq("de"), eq(FIRST_PAGE))).thenReturn(page);

      assertThat(service.listGreetings(" DE ", FIRST_PAGE)).isSameAs(page);
    }

    @Test
    void rejectsAnUnknownFilter() {
      assertThatThrownBy(() -> service.listGreetings("xx", FIRST_PAGE))
          .isInstanceOf(UnsupportedLanguageException.class);
    }
  }

  @Nested
  class DeleteGreeting {

    @Test
    void deletesWhenPresent() {
      when(repository.existsById(7L)).thenReturn(true);

      service.deleteGreeting(7L);

      verify(repository).deleteById(7L);
    }

    @Test
    void throwsAndDeletesNothingWhenAbsent() {
      when(repository.existsById(7L)).thenReturn(false);

      assertThatThrownBy(() -> service.deleteGreeting(7L))
          .isInstanceOf(GreetingNotFoundException.class)
          .extracting(thrown -> ((GreetingNotFoundException) thrown).getId())
          .isEqualTo(7L);

      verify(repository, never()).deleteById(anyLong());
    }
  }
}
