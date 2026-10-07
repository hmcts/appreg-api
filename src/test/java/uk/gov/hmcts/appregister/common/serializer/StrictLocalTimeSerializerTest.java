package uk.gov.hmcts.appregister.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StrictLocalTimeSerializerTest {

    @Test
    void testSerialize() throws Exception {
        StrictLocalTimeSerializer serializer = new StrictLocalTimeSerializer();

        LocalTime localTime = LocalTime.of(12, 30);
        JsonGenerator generator = Mockito.mock(JsonGenerator.class);
        serializer.serialize(localTime, generator, null);

        Mockito.verify(generator).writeString("12:30");
    }

    @Test
    void givenMidnightWithFiveMinutes_whenSerialize_thenWritesLeadingZeros() throws Exception {
        StrictLocalTimeSerializer serializer = new StrictLocalTimeSerializer();

        LocalTime localTime = LocalTime.of(0, 5);
        JsonGenerator generator = Mockito.mock(JsonGenerator.class);
        serializer.serialize(localTime, generator, null);

        Mockito.verify(generator).writeString("00:05");
    }

    @Test
    void givenSingleDigitHour_whenSerialize_thenWritesLeadingZero() throws Exception {
        StrictLocalTimeSerializer serializer = new StrictLocalTimeSerializer();

        LocalTime localTime = LocalTime.of(9, 5);
        JsonGenerator generator = Mockito.mock(JsonGenerator.class);
        serializer.serialize(localTime, generator, null);

        Mockito.verify(generator).writeString("09:05");
    }
}
