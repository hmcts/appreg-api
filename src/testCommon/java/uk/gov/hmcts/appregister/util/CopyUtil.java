package uk.gov.hmcts.appregister.util;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.PartialFilterData;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;

/**
 * A utility supporting deep copying.
 */
public class CopyUtil {
    protected static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Uses Jackson to deep clone the keyable.
     * @param keyable The keyable to clone.
     * @return The new deep clone.
     */
    public static <T> T deepClone(T keyable)  {
        try {
            return (T) objectMapper.readValue(objectMapper.writeValueAsString(keyable), keyable.getClass());
        } catch (JsonProcessingException jsonProcessingException) {
            throw new FilterProcessingException(jsonProcessingException);
        }
    }
}
