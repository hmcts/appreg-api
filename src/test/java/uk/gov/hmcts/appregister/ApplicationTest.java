package uk.gov.hmcts.appregister;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class ApplicationTest {

    @Test
    void constructor_createsApplicationInstance() {
        var application = assertDoesNotThrow(Application::new);
        assertNotNull(application);
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
