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
import uk.gov.hmcts.appregister.audit.listener.DataAuditLogger;
import uk.gov.hmcts.appregister.audit.listener.diff.JaversDifferentiator;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;

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

    @Value("${appreg.audit.diff.enable-complex-diff}")
    private boolean complexDiffEnabled;

    @Value("${appreg.audit.diff.enable-collection-diff}")
    private boolean collectionDiffEnabled;

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
     * load a preconfigured data audit logger that logs to the database based on a data
     * differentiator. The default differentiator is a reflective one that checks all fields for
     * differences. Reflective nesting of complex objects as well as collections are disabled by
     * default. This can be overridden as appropriate NOTE: This can be overridden at the operation
     * level. See {@link uk.gov.hmcts.appregister.audit.service.AuditOperationService}
     */
    @Bean
    public DataAuditLogger auditDifferentiator(DataAuditRepository dataAuditRepository) {
        return new DataAuditLogger(
                new JaversDifferentiator(complexDiffEnabled), dataAuditRepository);
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
