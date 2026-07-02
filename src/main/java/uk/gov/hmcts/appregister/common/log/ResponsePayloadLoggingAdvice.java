package uk.gov.hmcts.appregister.common.log;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Logs JSON response payloads for endpoints annotated with {@link LogPayloads}.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class ResponsePayloadLoggingAdvice implements ResponseBodyAdvice<Object> {
    private final PayloadLogSupport payloadLogSupport;

    @Override
    public boolean supports(
            MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return payloadLogSupport.findAnnotation(returnType).isPresent();
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (payloadLogSupport.shouldLogResponse(returnType, selectedContentType, body)) {
            payloadLogSupport
                    .findAnnotation(returnType)
                    .ifPresent(annotation -> payloadLogSupport.logResponse(annotation, body));
        }

        return body;
    }
}
