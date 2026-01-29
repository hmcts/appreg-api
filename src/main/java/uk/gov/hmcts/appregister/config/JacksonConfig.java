package uk.gov.hmcts.appregister.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeDeserializer;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeSerializer;

/**
 * This class sets up the Jackson 2 Object Mapper with the required modules for OpenAPI Dto
 * serialization/deserialisation. Spring Boot 4 expects us to use Jackson 3 but the jackson 3 API
 * does not natively support JsonNullable types. This will need to be addressed later in the
 * development cycle.
 */
@Deprecated
@Configuration
@RequiredArgsConstructor
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
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalTime.class, new StrictLocalTimeSerializer());
        module.addDeserializer(LocalTime.class, new StrictLocalTimeDeserializer());

        return builder -> builder.modulesToInstall(new JsonNullableModule(), module);
    }
}
