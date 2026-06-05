package uk.gov.hmcts.appregister.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;

class JacksonConfigTest {
    @Test
    void
            givenActivityAuditFilterContainsUnsupportedField_whenDeserialising_thenUnknownFieldIsRejected()
                    throws Exception {
        ObjectMapper objectMapper = createObjectMapper();
        String body =
                """
                {
                  "dateFrom": "2025-10-01",
                  "dateTo": "2025-10-31",
                  "activityTypes": [
                    "CREATE_APPLICATION_LIST"
                  ],
                  "courtCode": "LOC123"
                }
                """;

        UnrecognizedPropertyException exception =
                Assertions.assertThrows(
                        UnrecognizedPropertyException.class,
                        () -> objectMapper.readValue(body, ActivityAuditFilterDto.class));

        Assertions.assertEquals("courtCode", exception.getPropertyName());
    }

    @Test
    void
            givenFeesReportFilterContainsUnsupportedField_whenDeserialising_thenUnknownFieldIsRejected()
                    throws Exception {
        ObjectMapper objectMapper = createObjectMapper();
        String body =
                """
                {
                  "dateFrom": "2025-10-01",
                  "dateTo": "2025-10-31",
                  "courtCode": "LOC123"
                }
                """;

        UnrecognizedPropertyException exception =
                Assertions.assertThrows(
                        UnrecognizedPropertyException.class,
                        () -> objectMapper.readValue(body, FeesReportFilterDto.class));

        Assertions.assertEquals("courtCode", exception.getPropertyName());
    }

    @Test
    void givenSchemaAllowsAdditionalProperties_whenDeserialising_thenUnknownFieldIsIgnored()
            throws Exception {
        ObjectMapper objectMapper = createObjectMapper();
        String body =
                """
                {
                  "unsupportedField": "allowed by default"
                }
                """;

        ApplicationListCreateDto deserialised =
                objectMapper.readValue(body, ApplicationListCreateDto.class);

        Assertions.assertNotNull(deserialised);
    }

    @Test
    void givenOpenApiRequestBodySchemaIsStrict_whenResolvingStrictTypes_thenGeneratedDtoIsStrict() {
        Set<Class<?>> strictTypes =
                new OpenApiAdditionalPropertiesResolver(new PathMatchingResourcePatternResolver())
                        .requestBodyTypesDisallowingAdditionalProperties();

        Assertions.assertTrue(strictTypes.contains(ActivityAuditFilterDto.class));
        Assertions.assertTrue(strictTypes.contains(FeesReportFilterDto.class));
        Assertions.assertFalse(strictTypes.contains(ApplicationListCreateDto.class));
    }

    private ObjectMapper createObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        new JacksonConfig()
                .jsonNullableCustomizer(new PathMatchingResourcePatternResolver())
                .customize(builder);

        return builder.build();
    }
}
