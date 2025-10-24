package uk.gov.hmcts.appregister.common.util;

import java.lang.reflect.Method;

/**
 * A central place for cache to exist to avoid reflection based performance problems
 */
public class ReflectionCaches {
    public record ReflectionMeta(Method[] methods) {}

    /**
     * The method cache that is used for performance
     * @return The method cache for performance
     */
    public static final ClassValue<ReflectionMeta> METHOD_CACHE =
            new ClassValue<>() {
                @Override
                protected ReflectionMeta computeValue(Class<?> type) {
                    return new ReflectionMeta(
                            type.getDeclaredMethods()
                    );
                }
            };
}
