package uk.gov.hmcts.appregister.applicationentryresult.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;

class BulkResultDuplicateEntryIdsRequestBodyAdviceTest {

    private static final Class<? extends HttpMessageConverter<?>> CONVERTER_TYPE =
            MappingJackson2HttpMessageConverter.class;

    private final BulkResultDuplicateEntryIdsRequestBodyAdvice advice =
            new BulkResultDuplicateEntryIdsRequestBodyAdvice(new ObjectMapper());

    @Test
    void givenDuplicateEntryIds_whenBeforeBodyRead_thenDuplicateEntryIdsErrorIsThrown()
            throws Exception {
        UUID entryId = UUID.randomUUID();
        String body =
                """
                {
                  "entryIds": ["%s", "%s"],
                  "result": {
                    "resultCode": "RTC"
                  }
                }
                """
                        .formatted(entryId, entryId);

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                advice.beforeBodyRead(
                                        request(body), null, BulkResultDto.class, CONVERTER_TYPE));

        Assertions.assertEquals(
                ApplicationListEntryResultError.DUPLICATE_ENTRY_IDS.getCode(),
                exception.getCode().getCode());
    }

    @Test
    void givenUniqueEntryIds_whenBeforeBodyRead_thenBodyCanStillBeRead() throws Exception {
        UUID entryId = UUID.randomUUID();
        UUID entryId2 = UUID.randomUUID();
        String body =
                """
                {
                  "entryIds": ["%s", "%s"],
                  "result": {
                    "resultCode": "RTC"
                  }
                }
                """
                        .formatted(entryId, entryId2);

        HttpInputMessage inputMessage =
                advice.beforeBodyRead(request(body), null, BulkResultDto.class, CONVERTER_TYPE);

        Assertions.assertEquals(
                body, new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void givenBulkResultDtoTargetType_whenSupports_thenTrue() throws Exception {
        Method method = TestController.class.getDeclaredMethod("bulkResult", BulkResultDto.class);

        Assertions.assertTrue(
                advice.supports(null, method.getGenericParameterTypes()[0], CONVERTER_TYPE));
    }

    @Test
    void givenOtherTargetType_whenSupports_thenFalse() {
        Assertions.assertFalse(advice.supports(null, String.class, CONVERTER_TYPE));
    }

    private HttpInputMessage request(String body) {
        return new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
    }

    private static class TestController {

        @SuppressWarnings("unused")
        void bulkResult(BulkResultDto bulkResultDto) {
            throw new UnsupportedOperationException("Test helper only");
        }
    }
}
