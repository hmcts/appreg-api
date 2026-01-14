package uk.gov.hmcts.appregister.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Registers Jackson modules required for OpenAPI-generated models. JsonNullableModule: supports
     * fields of type JsonNullable
     *
     * <p>This method supports the ability for our rest API to use strings of the format "H:mm" when
     * serializing and deserializing LocalTime fields.
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonNullableCustomizer() {
        return builder ->
                builder.modulesToInstall(new JsonNullableModule())
                        .serializers(new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm")))
                        .deserializers(
                                new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm")));
    }
}
