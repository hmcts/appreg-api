package uk.gov.hmcts.appregister.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;

/**
 * Utility class for obfuscating sensitive data in logs. This is used to prevent sensitive data from
 * being logged in plain text, which can be a security risk.
 */
@Slf4j
public class ObfuscationUtil {
    private static final String REDACTED = "[REDACTED]";

    static final ObjectMapper mapper = new ObjectMapper();

    // register all of the serializers to the object mapper
    static {
        SimpleModule maskingModule = new SimpleModule();

        maskingModule.addSerializer(Person.class, new PersonSensitiveSerializer());
        maskingModule.addSerializer(Organisation.class, new OrganizationSensitiveSerializer());
        maskingModule.addSerializer(NameAddress.class, new NameAddressSensitiveSerializer());

        mapper.registerModule(maskingModule);
        mapper.registerModule(new JsonNullableModule());
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Uses jackson to anonymise PII data by targeting the {@link
     * uk.gov.hmcts.appregister.generated.model.Person} class or {@link
     * uk.gov.hmcts.appregister.generated.model.Organisation} or {@link
     * uk.gov.hmcts.appregister.common.entity.NameAddress}. This should be used when logging objects
     * that may contain PII data.
     *
     * @param o The object to be obfuscated.
     * @return The obfuscated string representation of the object.
     */
    public static String getObfuscatedString(Object o) {
        try {
            SimpleModule maskingModule = new SimpleModule();

            maskingModule.addSerializer(Person.class, new PersonSensitiveSerializer());
            maskingModule.addSerializer(Organisation.class, new OrganizationSensitiveSerializer());
            maskingModule.addSerializer(NameAddress.class, new NameAddressSensitiveSerializer());

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(maskingModule);
            mapper.registerModule(new JsonNullableModule());
            mapper.registerModule(new JavaTimeModule());

            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error(jsonProcessingException.getMessage(), jsonProcessingException);
        }

        return "Can't obfuscate object";
    }

    /** Serializer to redact Person PII data. */
    static class PersonSensitiveSerializer extends JsonSerializer<Person> {

        @Override
        public void serialize(Person value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(REDACTED);
        }
    }

    /** Serializer to redact Person PII data. */
    static class NameAddressSensitiveSerializer extends JsonSerializer<NameAddress> {

        @Override
        public void serialize(NameAddress value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(REDACTED);
        }
    }

    /** Serializer to redact Person PII data. */
    static class OrganizationSensitiveSerializer extends JsonSerializer<Organisation> {

        @Override
        public void serialize(Organisation value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(REDACTED);
        }
    }
}
