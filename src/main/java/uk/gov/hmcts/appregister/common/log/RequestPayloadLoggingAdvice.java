package uk.gov.hmcts.appregister.common.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.CharBuffer;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Logs JSON request payloads for endpoints annotated with {@link LogPayloads}.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class RequestPayloadLoggingAdvice extends RequestBodyAdviceAdapter {
    private final PayloadLogSupport payloadLogSupport;

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return payloadLogSupport.findAnnotation(methodParameter).isPresent();
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        if (payloadLogSupport.shouldLogRequest(
                parameter, inputMessage.getHeaders().getContentType(), body)) {
            payloadLogSupport
                    .findAnnotation(parameter)
                    .ifPresent(annotation -> payloadLogSupport.logRequest(annotation, body));
        }

        return body;
    }
}
