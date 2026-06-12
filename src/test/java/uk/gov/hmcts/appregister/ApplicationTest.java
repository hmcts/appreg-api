package uk.gov.hmcts.appregister;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class ApplicationTest {

    @Test
    void constructor_createsApplicationInstance() {
        new Application();
    }

    @Test
    void main_startsSpringApplication() {
        try (var springApplication = mockStatic(SpringApplication.class)) {
            Application.main(new String[] {"--debug"});

            springApplication.verify(
                    () -> SpringApplication.run(Application.class, new String[] {"--debug"}));
        }
    }
}
