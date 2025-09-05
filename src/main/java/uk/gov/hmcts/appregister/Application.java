package uk.gov.hmcts.appregister;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The entry point into the application. */
@SpringBootApplication
@SuppressWarnings(
    "HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

  public static void main(final String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
