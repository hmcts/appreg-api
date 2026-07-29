package uk.gov.hmcts.appregister.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApiConstantsTest {

    @Test
    void mediaTypes_exposesExpectedValues() {
        assertEquals(
                ApiConstants.MediaTypes.VND_JSON_V1,
                MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json"));
        assertEquals(ApiConstants.MediaTypes.TEXT_CSV, MediaType.parseMediaType("text/csv"));
    }

    @Test
    void apiConstants_constructorThrowsUtilityClassException() throws Exception {
        var constructor = ApiConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        var exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
        assertEquals("Utility class", exception.getCause().getMessage());
    }

    @Test
    void mediaTypes_constructorThrowsUtilityClassException() throws Exception {
        var constructor = ApiConstants.MediaTypes.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        var exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertEquals(UnsupportedOperationException.class, exception.getCause().getClass());
        assertEquals("Utility class", exception.getCause().getMessage());
    }
}
