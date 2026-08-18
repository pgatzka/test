/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.pgatzka.greeting.domain.Greeting;
import io.github.pgatzka.greeting.domain.GreetingRepository;

/** Renders greetings and keeps a record of every one that was issued. */
@Service
@Transactional(readOnly = true)
public class GreetingService {

  private final GreetingRepository repository;

  public GreetingService(GreetingRepository repository) {
    this.repository = repository;
  }

  /**
   * Renders a greeting for {@code recipient} and stores it.
   *
   * @param recipient who to greet; surrounding whitespace is trimmed
   * @param languageCode ISO 639-1 code, {@code null} or blank defaults to English
   * @return the persisted greeting, with its generated identifier
   * @throws UnsupportedLanguageException when the code is not one this service knows
   */
  @Transactional
  public Greeting createGreeting(String recipient, String languageCode) {
    Language language = resolveLanguage(languageCode);
    String trimmedRecipient = recipient.trim();
    Greeting greeting =
        new Greeting(trimmedRecipient, language.code(), language.render(trimmedRecipient));
    return repository.save(greeting);
  }

  /**
   * @throws GreetingNotFoundException when no greeting has that identifier
   */
  public Greeting getGreeting(long id) {
    return repository.findById(id).orElseThrow(() -> new GreetingNotFoundException(id));
  }

  /**
   * Lists stored greetings, optionally narrowed to a single language.
   *
   * @param languageCode filter, or {@code null}/blank for all languages
   */
  public Page<Greeting> listGreetings(String languageCode, Pageable pageable) {
    if (languageCode == null || languageCode.isBlank()) {
      return repository.findAll(pageable);
    }
    return repository.findByLanguageCode(resolveLanguage(languageCode).code(), pageable);
  }

  /**
   * @throws GreetingNotFoundException when no greeting has that identifier
   */
  @Transactional
  public void deleteGreeting(long id) {
    if (!repository.existsById(id)) {
      throw new GreetingNotFoundException(id);
    }
    repository.deleteById(id);
  }

  /** Blank means "caller did not care", which we read as English rather than as an error. */
  private Language resolveLanguage(String languageCode) {
    if (languageCode == null || languageCode.isBlank()) {
      return Language.EN;
    }
    return Language.fromCode(languageCode)
        .orElseThrow(() -> new UnsupportedLanguageException(languageCode));
  }
}
