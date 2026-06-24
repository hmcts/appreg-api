package uk.gov.hmcts.appregister.common.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * A custom deserializer for LocalTime that strictly enforces the "HH:mm" format. Only supports
 * deserialization of LocalTime from strings in "HH:mm" format. If an array or any other format is
 * encountered, an exception is thrown.
 */
@Slf4j
public class StrictLocalTimeDeserializer extends JsonDeserializer<LocalTime> {
    private static final HttpInputMessage EMPTY_INPUT_MESSAGE =
            new HttpInputMessage() {
                @Override
                public InputStream getBody() {
                    return InputStream.nullInputStream();
                }

                @Override
                public HttpHeaders getHeaders() {
                    return HttpHeaders.EMPTY;
                }
            };

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var token = p.currentToken();

        if (token == JsonToken.START_ARRAY) {
            throw new HttpMessageNotReadableException(
                    "Unexpected time format detected %s".formatted(getErroneousArrayString(p)),
                    EMPTY_INPUT_MESSAGE);
        }

        // Accept string only (you can also accept NUMBER if you want, but here we do not)
        if (token == JsonToken.VALUE_STRING) {
            var text = p.getText().trim();

            return LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm"));
        }

        throw new HttpMessageNotReadableException(
                "Unexpected time format detected", EMPTY_INPUT_MESSAGE);
    }

    /**
     * gets the erroneous array value as a string.
     *
     * @param p The parse to get the value
     * @return The string
     */
    private String getErroneousArrayString(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            return String.valueOf(p.getText());
        }

        var sb = new StringBuilder("[");
        var first = true;

        while (p.nextToken() != JsonToken.END_ARRAY) {
            if (!first) {
                sb.append(',');
            }
            first = false;

            // This gives the actual scalar value text at the current position
            sb.append(p.getValueAsString(p.getText()));
        }

        sb.append(']');
        return sb.toString();
    }
}
