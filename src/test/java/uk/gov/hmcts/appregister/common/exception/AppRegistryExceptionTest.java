package uk.gov.hmcts.appregister.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppRegistryExceptionTest {

    @Test
    void constructor_copiesDetailsMap() {
        var details = new HashMap<>(Map.of("invalid_entry_ids", "1,2,3"));

        var exception =
                new AppRegistryException(CommonAppError.NOT_READABLE_ERROR, "detail", details);

        assertNotSame(details, exception.getDetails());
        assertEquals(details, exception.getDetails());

        details.put("invalid_entry_ids", "changed");
        assertEquals("1,2,3", exception.getDetails().get("invalid_entry_ids"));
    }
}
