package uk.gov.hmcts.appregister.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.timezone:Europe/London}")
    private String timezone;

    /**
     * Allows the clock to be modified so that we can test time sensitive code.
     *
     * @return The clock that is returned which is a UTC clock
     */
    @Bean
    public Clock getClock() {
        return Clock.systemUTC();
    }

    @Bean
    DateTimeProvider utcDateTimeProvider(Clock clock) {
        return () -> java.util.Optional.of(OffsetDateTime.now(clock));
    }

    @Bean
    public ZoneId ukZone() {
        return ZoneId.of(timezone);
    }

    /**
     * Defines a audit lifecycle listener that can be called when working with the {@link
     * uk.gov.hmcts.appregister.audit.service.AuditOperationService}.
     */
    @Bean
    public AuditOperationLifecycleListener getSystemLevelAuditListener() {
        return new AuditOperationSlf4jLogger();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new ListStringConverter());
    }

    /**
     * A converter to convert a single string into a list. This avoids delineating strings by comma.
     * This is useful for sort pagination
     */
    class ListStringConverter implements GenericConverter {
        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Set.of(new ConvertiblePair(String.class, List.class));
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source instanceof String str) {
                return List.of(str);
            }
            return null;
        }
    }
}
