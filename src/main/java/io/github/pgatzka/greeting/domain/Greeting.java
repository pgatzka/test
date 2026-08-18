/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A greeting that was rendered for someone and stored.
 *
 * <p>The schema is owned by Flyway (see {@code db/migration}); Hibernate only validates against it.
 */
@Entity
@Table(name = "greeting")
@EntityListeners(AuditingEntityListener.class)
public class Greeting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "recipient", nullable = false, length = 100)
  private String recipient;

  @Column(name = "language_code", nullable = false, length = 8)
  private String languageCode;

  @Column(name = "message", nullable = false, length = 255)
  private String message;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Required by JPA. */
  protected Greeting() {}

  public Greeting(String recipient, String languageCode, String message) {
    this.recipient = recipient;
    this.languageCode = languageCode;
    this.message = message;
  }

  public Long getId() {
    return id;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getLanguageCode() {
    return languageCode;
  }

  public String getMessage() {
    return message;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Entity equality is identifier-based; two unsaved instances are never equal.
   *
   * <p>This is the standard JPA contract: a transient entity has no identity yet, so it can only be
   * equal to itself.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Greeting greeting)) {
      return false;
    }
    return id != null && id.equals(greeting.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "Greeting{id=%s, recipient='%s', languageCode='%s'}"
        .formatted(id, recipient, languageCode);
  }
}
