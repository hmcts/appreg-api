package uk.gov.hmcts.appregister.common.serializer;

import com.fasterxml.jackson.databind.JsonSerializer;
import java.time.LocalTime;

/**
 * A custom serializer for LocalTime that strictly enforces the "HH:mm" format. Serializes LocalTime
 * to strings in "HH:mm" format.
 */
public class StrictLocalTimeSerializer extends JsonSerializer<LocalTime> {
    @Override
    public void serialize(
            LocalTime value,
            com.fasterxml.jackson.core.JsonGenerator gen,
            com.fasterxml.jackson.databind.SerializerProvider serializers)
            throws java.io.IOException {
        // Serialize LocalTime to string in "HH:mm" format
        String timeString = value.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        gen.writeString(timeString);
    }
}
