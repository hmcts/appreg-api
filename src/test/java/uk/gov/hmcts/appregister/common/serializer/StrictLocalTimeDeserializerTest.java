package uk.gov.hmcts.appregister.common.serializer;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.time.LocalTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.HttpMessageNotReadableException;

public class StrictLocalTimeDeserializerTest {

    @Test
    void testDeserialize() throws Exception {
        JsonParser parser = Mockito.mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(parser.getText()).thenReturn("12:30");

        StrictLocalTimeDeserializer deserializer = new StrictLocalTimeDeserializer();

        Assertions.assertEquals(
                LocalTime.parse("12:30:00"), deserializer.deserialize(parser, null));
    }

    @Test
    void testDeserializeFail() throws Exception {
        JsonParser parser = Mockito.mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.START_ARRAY);
        when(parser.nextToken()).thenReturn(JsonToken.END_ARRAY);

        when(parser.getText()).thenReturn("12:30");

        StrictLocalTimeDeserializer deserializer = new StrictLocalTimeDeserializer();

        Assertions.assertThrows(
                HttpMessageNotReadableException.class,
                () -> deserializer.deserialize(parser, null));
    }
}
