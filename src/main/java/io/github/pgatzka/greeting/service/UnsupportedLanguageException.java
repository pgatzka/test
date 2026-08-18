/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting.service;

/** Raised when a caller asks for a language the service does not know. */
public class UnsupportedLanguageException extends RuntimeException {

  private final String requestedCode;

  public UnsupportedLanguageException(String requestedCode) {
    super(
        "Unsupported language '%s'. Supported languages: %s."
            .formatted(requestedCode, Language.supportedCodes()));
    this.requestedCode = requestedCode;
  }

  public String getRequestedCode() {
    return requestedCode;
  }
}
