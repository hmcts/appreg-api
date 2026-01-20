package uk.gov.hmcts.appregister.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalTime;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeDeserializer;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeSerializer;

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
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalTime.class, new StrictLocalTimeSerializer());
        module.addDeserializer(LocalTime.class, new StrictLocalTimeDeserializer());

        return builder -> builder.modulesToInstall(new JsonNullableModule(), module);
    }
}
