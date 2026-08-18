/* (C) 2026 pgatzka - Released under the MIT License. */
package io.github.pgatzka.greeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the greeting service. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GreetingApplication {

  public static void main(String[] args) {
    SpringApplication.run(GreetingApplication.class, args);
  }
}
