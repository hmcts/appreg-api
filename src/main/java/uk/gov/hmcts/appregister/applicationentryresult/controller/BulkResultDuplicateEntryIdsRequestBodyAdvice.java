package uk.gov.hmcts.appregister.applicationentryresult.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;

@ControllerAdvice
@RequiredArgsConstructor
public class BulkResultDuplicateEntryIdsRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private static final String ENTRY_IDS_FIELD = "entryIds";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return BulkResultDto.class.equals(ResolvableType.forType(targetType).resolve());
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();

        rejectDuplicateEntryIds(body);

        return new BufferedHttpInputMessage(body, inputMessage.getHeaders());
    }

    private void rejectDuplicateEntryIds(byte[] body) throws IOException {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(body);
        } catch (JsonProcessingException ignored) {
            return;
        }

        if (rootNode == null || !rootNode.has(ENTRY_IDS_FIELD)) {
            return;
        }

        JsonNode entryIdsNode = rootNode.get(ENTRY_IDS_FIELD);
        if (!entryIdsNode.isArray()) {
            return;
        }

        Set<UUID> uniqueEntryIds = new HashSet<>();
        for (JsonNode entryIdNode : entryIdsNode) {
            UUID entryId = parseEntryId(entryIdNode);
            if (entryId == null) {
                continue;
            }
            if (!uniqueEntryIds.add(entryId)) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.DUPLICATE_ENTRY_IDS,
                        "Duplicate entry IDs are not allowed");
            }
        }
    }

    private UUID parseEntryId(JsonNode entryIdNode) {
        try {
            return objectMapper.treeToValue(entryIdNode, UUID.class);
        } catch (IllegalArgumentException | JsonProcessingException ignored) {
            return null;
        }
    }

    private record BufferedHttpInputMessage(byte[] body, HttpHeaders headers)
            implements HttpInputMessage {

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
