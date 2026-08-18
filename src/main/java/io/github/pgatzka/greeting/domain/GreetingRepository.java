/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Greeting}. */
public interface GreetingRepository extends JpaRepository<Greeting, Long> {

  /** Finds greetings issued in one language, newest first is controlled by the caller's sort. */
  Page<Greeting> findByLanguageCode(String languageCode, Pageable pageable);
}
