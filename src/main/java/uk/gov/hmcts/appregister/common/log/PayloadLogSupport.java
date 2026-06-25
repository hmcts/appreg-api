package uk.gov.hmcts.appregister.common.log;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import uk.gov.hmcts.appregister.common.util.ObfuscationUtil;

/**
 * Shared payload logging helpers for annotation-driven request/response logging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayloadLogSupport {
    private final ConcurrentMap<Method, Optional<LogPayloads>> annotationCache =
            new ConcurrentHashMap<>();

    private final JsonPayloadDetector jsonPayloadDetector;

    public Optional<LogPayloads> findAnnotation(MethodParameter parameter) {
        Method method = parameter.getMethod();
        if (method == null) {
            return Optional.empty();
        }

        Class<?> containingClass = parameter.getContainingClass();
        Class<?> userClass = ClassUtils.getUserClass(containingClass);
        Method specificMethod = resolveSpecificMethod(method, userClass);

        return annotationCache.computeIfAbsent(specificMethod, this::findMergedAnnotation);
    }

    public boolean shouldLogRequest(MethodParameter parameter, MediaType mediaType, Object body) {
        return shouldLog(parameter, mediaType, body, true);
    }

    public boolean shouldLogResponse(MethodParameter parameter, MediaType mediaType, Object body) {
        return shouldLog(parameter, mediaType, body, false);
    }

    public void logRequest(LogPayloads annotation, Object body) {
        log(annotation.level(), annotation.requestPrefix(), body);
    }

    public void logResponse(LogPayloads annotation, Object body) {
        log(annotation.level(), annotation.responsePrefix(), body);
    }

    private boolean shouldLog(
            MethodParameter parameter, MediaType mediaType, Object body, boolean request) {
        if (body == null || !jsonPayloadDetector.isJson(mediaType)) {
            return false;
        }

        return findAnnotation(parameter)
                .map(LogPayloads::direction)
                .map(
                        direction ->
                                request
                                        ? direction.includesRequest()
                                        : direction.includesResponse())
                .orElse(false);
    }

    private void log(PayloadLogLevel level, String prefix, Object body) {
        level.log(log, "{}: {}", prefix, ObfuscationUtil.getObfuscatedString(body));
    }

    private Method resolveSpecificMethod(Method method, Class<?> userClass) {
        return BridgeMethodResolver.findBridgedMethod(
                AopUtils.getMostSpecificMethod(method, userClass));
    }

    private Optional<LogPayloads> findMergedAnnotation(Method method) {
        return Optional.ofNullable(
                AnnotatedElementUtils.findMergedAnnotation(method, LogPayloads.class));
    }
}
