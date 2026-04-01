package uk.gov.hmcts.appregister.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;

/**
 * A utility supporting deep copying using Jackson.
 */
public class CopyUtil {
    protected static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * registers a mixin to the underlying ObjectMapper. This can be used for programmatic ignoring
     * of fields.
     *
     * @param clazz The class to register the mixin to.
     * @param mixin The mixin to register.
     */
    public static void registerMixin(Class<?> clazz, Class<?> mixin) {
        objectMapper.addMixIn(clazz, mixin);
    }

    /**
     * Uses Jackson to deep clone the keyable.
     *
     * @param keyable The keyable to clone.
     * @return The new deep clone.
     */
    public static <T> T deepClone(T keyable) {
        try {
            return (T)
                    objectMapper.readValue(
                            objectMapper.writeValueAsString(keyable), keyable.getClass());
        } catch (JsonProcessingException jsonProcessingException) {
            throw new FilterProcessingException(jsonProcessingException);
        }
    }
}
