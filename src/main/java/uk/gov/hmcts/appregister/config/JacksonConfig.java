package uk.gov.hmcts.appregister.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Registers Jackson modules required for OpenAPI-generated models. JsonNullableModule: supports
     * fields of type JsonNullable<T>
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonNullableCustomizer() {
        return builder -> builder.modulesToInstall(new JsonNullableModule());
    }
}
