package uk.gov.hmcts.appregister.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;

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

    private ObjectMapper createObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        new JacksonConfig().jsonNullableCustomizer().customize(builder);

        return builder.build();
    }
}
