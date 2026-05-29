package uk.gov.hmcts.appregister.report.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import uk.gov.hmcts.appregister.common.exception.AppRegExceptionHandler;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;

class ActivityAuditFilterRequestBodyAdviceTest {
    private final ActivityAuditFilterRequestBodyAdvice advice =
            new ActivityAuditFilterRequestBodyAdvice(new ObjectMapper());

    @Test
    void givenActivityAuditRequestHasUnsupportedField_whenBodyRead_thenNotReadableExceptionThrown()
            throws Exception {
        byte[] body =
                """
                {
                  "dateFrom": "2025-10-01",
                  "dateTo": "2025-10-31",
                  "activityTypes": [
                    "CREATE_APPLICATION_LIST"
                  ],
                  "courtCode": "LOC123"
                }
                """
                        .getBytes(StandardCharsets.UTF_8);

        HttpMessageNotReadableException exception =
                assertThrows(
                        HttpMessageNotReadableException.class,
                        () ->
                                advice.beforeBodyRead(
                                        new MockHttpInputMessage(body),
                                        null,
                                        ActivityAuditFilterDto.class,
                                        MappingJackson2HttpMessageConverter.class));

        UnrecognizedPropertyException cause =
                AppRegExceptionHandler.findCause(exception, UnrecognizedPropertyException.class);
        assertEquals("courtCode", cause.getPropertyName());
    }

    @Test
    void givenActivityAuditRequestHasOnlySupportedFields_whenBodyRead_thenBodyIsPreserved()
            throws Exception {
        byte[] body =
                """
                {
                  "dateFrom": "2025-10-01",
                  "dateTo": "2025-10-31",
                  "activityTypes": [
                    "CREATE_APPLICATION_LIST"
                  ]
                }
                """
                        .getBytes(StandardCharsets.UTF_8);

        var inputMessage =
                advice.beforeBodyRead(
                        new MockHttpInputMessage(body),
                        null,
                        ActivityAuditFilterDto.class,
                        MappingJackson2HttpMessageConverter.class);

        assertArrayEquals(body, inputMessage.getBody().readAllBytes());
    }
}
