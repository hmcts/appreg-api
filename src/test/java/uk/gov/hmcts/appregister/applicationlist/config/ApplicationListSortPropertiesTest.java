package uk.gov.hmcts.appregister.applicationlist.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;

class ApplicationListSortPropertiesTest {

    @Test
    void shouldMapDisabledSortKeysToEnums() {
        ApplicationListSortProperties properties = new ApplicationListSortProperties();
        properties.setDisabledSortKeys(List.of("time", "location", "description"));

        Set<ApplicationListSortFieldEnum> disabledEnums = properties.getDisabledEnums();

        assertThat(disabledEnums)
                .containsExactlyInAnyOrder(
                        ApplicationListSortFieldEnum.TIME,
                        ApplicationListSortFieldEnum.LOCATION,
                        ApplicationListSortFieldEnum.DESCRIPTION);
    }

    @Test
    void shouldIgnoreUnknownAndBlankDisabledSortKeys() {
        ApplicationListSortProperties properties = new ApplicationListSortProperties();
        properties.setDisabledSortKeys(
                Arrays.asList("time", " ", "not-a-real-key", null, "description"));

        Set<ApplicationListSortFieldEnum> disabledEnums = properties.getDisabledEnums();

        assertThat(disabledEnums)
                .containsExactlyInAnyOrder(
                        ApplicationListSortFieldEnum.TIME,
                        ApplicationListSortFieldEnum.DESCRIPTION);
    }

    @Test
    void validateShouldWarnForUnknownDisabledSortKeysButStillStartNormally() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApplicationListSortProperties.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ApplicationListSortProperties properties = new ApplicationListSortProperties();
            properties.setDisabledSortKeys(Arrays.asList("time", "bad-key", ""));

            assertDoesNotThrow(properties::validate);

            assertThat(appender.list)
                    .anySatisfy(
                            event -> {
                                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                                assertThat(event.getFormattedMessage())
                                        .contains("Unknown disabled sort key");
                            });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
