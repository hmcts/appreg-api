package uk.gov.hmcts.appregister.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeDeserializer;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeSerializer;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;

/**
 * This class sets up the Jackson 2 Object Mapper with the required modules for OpenAPI Dto
 * serialization/deserialisation. Spring Boot 4 expects us to use Jackson 3 but the jackson 3 API
 * does not natively support JsonNullable types. This will need to be addressed later in the
 * development cycle.
 */
@Deprecated
@SuppressWarnings("removal")
@Configuration
@RequiredArgsConstructor
public class JacksonConfig {
    private static final Set<Class<?>> STRICT_REQUEST_BODY_TYPES =
            Set.of(ActivityAuditFilterDto.class);

    /**
     * Registers Jackson modules required for OpenAPI-generated models. JsonNullableModule: supports
     * fields of type JsonNullable
     *
     * <p>This method supports the ability for our rest API to use strings of the format "HH:mm"
     * when serializing and deserializing LocalTime fields.
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonNullableCustomizer() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalTime.class, new StrictLocalTimeSerializer());
        module.addDeserializer(LocalTime.class, new StrictLocalTimeDeserializer());

        return builder -> configureBuilder(builder, module);
    }

    private void configureBuilder(
            Jackson2ObjectMapperBuilder builder, JavaTimeModule javaTimeModule) {
        builder.modulesToInstall(new JsonNullableModule(), javaTimeModule);
        builder.postConfigurer(JacksonConfig::configureObjectMapper);
    }

    private static void configureObjectMapper(ObjectMapper objectMapper) {
        objectMapper.addHandler(new StrictRequestBodyPropertyHandler());
    }

    private static class StrictRequestBodyPropertyHandler extends DeserializationProblemHandler {
        @Override
        public boolean handleUnknownProperty(
                DeserializationContext context,
                JsonParser parser,
                JsonDeserializer<?> deserializer,
                Object beanOrClass,
                String propertyName)
                throws IOException {
            Class<?> targetClass = resolveTargetClass(beanOrClass);
            if (STRICT_REQUEST_BODY_TYPES.contains(targetClass)) {
                throw UnrecognizedPropertyException.from(
                        parser, targetClass, propertyName, knownProperties(deserializer));
            }

            return false;
        }

        private Class<?> resolveTargetClass(Object beanOrClass) {
            if (beanOrClass instanceof Class<?> clazz) {
                return clazz;
            }
            if (beanOrClass == null) {
                return null;
            }

            return beanOrClass.getClass();
        }

        private Collection<Object> knownProperties(JsonDeserializer<?> deserializer) {
            if (deserializer == null) {
                return List.of();
            }

            Collection<Object> knownProperties = deserializer.getKnownPropertyNames();
            if (knownProperties == null) {
                return List.of();
            }

            return knownProperties;
        }
    }
}
