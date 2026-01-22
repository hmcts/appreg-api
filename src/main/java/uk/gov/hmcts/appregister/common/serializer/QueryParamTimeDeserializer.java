package uk.gov.hmcts.appregister.common.serializer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * A spring based converter to convert query time parameters to LocalTime so that
 * the time only has a hour and minute never seconds.
 */
@Component
public class QueryParamTimeDeserializer implements Converter<String, LocalTime> {
    @Override
    public LocalTime convert(String source) {
        return LocalTime.parse(source, DateTimeFormatter.ofPattern("HH:mm"));
    }
}
