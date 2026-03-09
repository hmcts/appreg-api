package uk.gov.hmcts.appregister.common.serializer;

import java.time.LocalTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryParamTimeDeserializerTest {

    @Test
    void testDeserialize() throws Exception {
        Assertions.assertEquals(
                LocalTime.parse("12:30:00"), new QueryParamTimeDeserializer().convert("12:30"));
    }
}
