package uk.gov.hmcts.appregister.report.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;

@ControllerAdvice
@RequiredArgsConstructor
class ActivityAuditFilterRequestBodyAdvice extends RequestBodyAdviceAdapter {
    private static final List<String> SUPPORTED_FIELDS =
            List.of("dateFrom", "dateTo", "username", "activityTypes");
    private static final Set<String> SUPPORTED_FIELD_NAMES = Set.copyOf(SUPPORTED_FIELDS);
    private static final List<Object> SUPPORTED_PROPERTY_IDS = List.copyOf(SUPPORTED_FIELDS);

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ActivityAuditFilterDto.class.equals(methodParameter.getParameterType())
                || ActivityAuditFilterDto.class.equals(targetType);
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
        validateSupportedFields(inputMessage, body);
        return new CachedBodyHttpInputMessage(inputMessage.getHeaders(), body);
    }

    private void validateSupportedFields(HttpInputMessage inputMessage, byte[] body)
            throws IOException {
        if (body.length == 0) {
            return;
        }

        try (JsonParser parser = objectMapper.getFactory().createParser(body)) {
            JsonNode request = objectMapper.readTree(parser);
            if (request == null || !request.isObject()) {
                return;
            }

            var fieldNames = request.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (!SUPPORTED_FIELD_NAMES.contains(fieldName)) {
                    UnrecognizedPropertyException exception =
                            UnrecognizedPropertyException.from(
                                    parser,
                                    ActivityAuditFilterDto.class,
                                    fieldName,
                                    SUPPORTED_PROPERTY_IDS);

                    throw new HttpMessageNotReadableException(
                            "Unsupported request field: " + fieldName, exception, inputMessage);
                }
            }
        } catch (JsonProcessingException ignored) {
            // Leave malformed JSON and other parsing failures to the normal message converter.
        }
    }

    private static class CachedBodyHttpInputMessage implements HttpInputMessage {
        private final HttpHeaders headers;
        private final byte[] body;

        private CachedBodyHttpInputMessage(HttpHeaders headers, byte[] body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
