/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.service;

/** Raised when no greeting exists for the requested identifier. */
public class GreetingNotFoundException extends RuntimeException {

  private final long id;

  public GreetingNotFoundException(long id) {
    super("Greeting %d not found.".formatted(id));
    this.id = id;
  }

  public long getId() {
    return id;
  }
}
