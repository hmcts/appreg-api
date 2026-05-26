package uk.gov.hmcts.appregister.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;

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
        maskingModule.addSerializer(
                EntryGetDetailDto.class, new EntryGetDetailDtoSensitiveSerializer());
        maskingModule.addSerializer(
                EntryGetPrintDto.class, new EntryGetPrintDtoSensitiveSerializer());
        maskingModule.addSerializer(EntryCreateDto.class, new EntryCreateDtoSensitiveSerializer());
        maskingModule.addSerializer(
                EntryGetSummaryDto.class, new EntryGetSummaryDtoSensitiveSerializer());
        maskingModule.addSerializer(EntryPage.class, new EntryPageSensitiveSerializer());
        maskingModule.addSerializer(
                ApplicationListEntrySummary.class,
                new ApplicationListEntrySummarySensitiveSerializer());
        maskingModule.addSerializer(
                ApplicationListGetDetailDto.class,
                new ApplicationListGetDetailDtoSensitiveSerializer());
        maskingModule.addSerializer(ResultGetDto.class, new ResultGetDtoSensitiveSerializer());
        maskingModule.addSerializer(ResultPage.class, new ResultPageSensitiveSerializer());

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
            maskingModule.addSerializer(
                    EntryGetDetailDto.class, new EntryGetDetailDtoSensitiveSerializer());
            maskingModule.addSerializer(
                    EntryGetPrintDto.class, new EntryGetPrintDtoSensitiveSerializer());
            maskingModule.addSerializer(
                    EntryCreateDto.class, new EntryCreateDtoSensitiveSerializer());
            maskingModule.addSerializer(
                    EntryGetSummaryDto.class, new EntryGetSummaryDtoSensitiveSerializer());
            maskingModule.addSerializer(EntryPage.class, new EntryPageSensitiveSerializer());
            maskingModule.addSerializer(
                    ApplicationListEntrySummary.class,
                    new ApplicationListEntrySummarySensitiveSerializer());
            maskingModule.addSerializer(
                    ApplicationListGetDetailDto.class,
                    new ApplicationListGetDetailDtoSensitiveSerializer());
            maskingModule.addSerializer(ResultGetDto.class, new ResultGetDtoSensitiveSerializer());
            maskingModule.addSerializer(ResultPage.class, new ResultPageSensitiveSerializer());

            ObjectMapper mapper = new ObjectMapper();
            mapper.setConfig(
                    mapper.getSerializationConfig()
                            .with(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_OPTIONALS));
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

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

    /** Serializer to redact EntryGetDetailDto PII data. */
    static class EntryGetDetailDtoSensitiveSerializer extends JsonSerializer<EntryGetDetailDto> {

        @Override
        public void serialize(
                EntryGetDetailDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(REDACTED);
        }
    }

    /** Serializer to redact EntryGetPrintDto PII data. */
    static class EntryGetPrintDtoSensitiveSerializer extends JsonSerializer<EntryGetPrintDto> {

        @Override
        public void serialize(
                EntryGetPrintDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(REDACTED);
        }
    }

    /** Serializer to redact EntryCreateDto PII data. */
    static class EntryCreateDtoSensitiveSerializer extends JsonSerializer<EntryCreateDto> {

        @Override
        public void serialize(
                EntryCreateDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(REDACTED);
        }
    }

    /** Serializer to redact EntryGetSummaryDto PII data. */
    static class EntryGetSummaryDtoSensitiveSerializer extends JsonSerializer<EntryGetSummaryDto> {

        @Override
        public void serialize(
                EntryGetSummaryDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("id", value.getId());
            gen.writeObjectField("applicant", REDACTED);
            gen.writeObjectField("respondent", REDACTED);
            gen.writeStringField("applicationTitle", value.getApplicationTitle());
            gen.writeObjectField("legislation", value.getLegislation());
            gen.writeObjectField("isFeeRequired", value.getIsFeeRequired());
            gen.writeObjectField("isResulted", value.getIsResulted());
            gen.writeObjectField("listId", value.getListId());
            gen.writeObjectField("date", value.getDate());
            gen.writeObjectField("sequenceNumber", value.getSequenceNumber());
            gen.writeObjectField("resulted", value.getResulted());
            gen.writeObjectField("status", value.getStatus());

            if (value.getAccountNumber() != null && value.getAccountNumber().isPresent()) {
                gen.writeStringField("accountNumber", REDACTED);
            } else {
                gen.writeNullField("accountNumber");
            }

            gen.writeEndObject();
        }
    }

    /** Serializer to redact EntryPage PII data. */
    static class EntryPageSensitiveSerializer extends JsonSerializer<EntryPage> {

        @Override
        public void serialize(EntryPage value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("pageNumber", value.getPageNumber());
            gen.writeObjectField("pageSize", value.getPageSize());
            gen.writeObjectField("totalElements", value.getTotalElements());
            gen.writeObjectField("totalPages", value.getTotalPages());
            gen.writeObjectField("sort", value.getSort());
            gen.writeObjectField("first", value.getFirst());
            gen.writeObjectField("last", value.getLast());
            gen.writeObjectField("elementsOnPage", value.getElementsOnPage());
            gen.writeObjectField("content", value.getContent());
            gen.writeEndObject();
        }
    }

    static class ApplicationListEntrySummarySensitiveSerializer
            extends JsonSerializer<ApplicationListEntrySummary> {

        @Override
        public void serialize(
                ApplicationListEntrySummary value,
                JsonGenerator gen,
                SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("uuid", value.getUuid());
            gen.writeObjectField("sequenceNumber", value.getSequenceNumber());

            gen.writeStringField("accountNumber", REDACTED);
            gen.writeStringField("applicant", REDACTED);
            gen.writeStringField("respondent", REDACTED);
            gen.writeStringField("postCode", REDACTED);

            gen.writeObjectField("applicationTitle", value.getApplicationTitle());
            gen.writeObjectField("feeRequired", value.getFeeRequired());
            gen.writeObjectField("result", value.getResult());
            gen.writeEndObject();
        }
    }

    static class ApplicationListGetDetailDtoSensitiveSerializer
            extends JsonSerializer<ApplicationListGetDetailDto> {

        @Override
        public void serialize(
                ApplicationListGetDetailDto value,
                JsonGenerator gen,
                SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("id", value.getId());
            gen.writeObjectField("date", value.getDate());
            gen.writeObjectField("time", value.getTime());
            gen.writeObjectField("description", value.getDescription());
            gen.writeObjectField("status", value.getStatus());
            gen.writeObjectField("courtCode", value.getCourtCode());
            gen.writeObjectField("courtName", value.getCourtName());
            gen.writeObjectField("cjaCode", value.getCjaCode());
            gen.writeObjectField("otherLocationDescription", value.getOtherLocationDescription());
            gen.writeObjectField("durationHours", value.getDurationHours());
            gen.writeObjectField("durationMinutes", value.getDurationMinutes());
            gen.writeObjectField("version", value.getVersion());
            gen.writeObjectField("entriesCount", value.getEntriesCount());
            gen.writeObjectField("entriesSummary", value.getEntriesSummary());
            gen.writeEndObject();
        }
    }

    static class ResultGetDtoSensitiveSerializer extends JsonSerializer<ResultGetDto> {

        @Override
        public void serialize(ResultGetDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("id", value.getId());
            gen.writeObjectField("entryId", value.getEntryId());
            gen.writeObjectField("resultCode", value.getResultCode());
            gen.writeObjectField("wording", value.getWording());
            gen.writeEndObject();
        }
    }

    static class ResultPageSensitiveSerializer extends JsonSerializer<ResultPage> {

        @Override
        public void serialize(ResultPage value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("pageNumber", value.getPageNumber());
            gen.writeObjectField("pageSize", value.getPageSize());
            gen.writeObjectField("totalElements", value.getTotalElements());
            gen.writeObjectField("totalPages", value.getTotalPages());
            gen.writeObjectField("sort", value.getSort());
            gen.writeObjectField("first", value.getFirst());
            gen.writeObjectField("last", value.getLast());
            gen.writeObjectField("elementsOnPage", value.getElementsOnPage());
            gen.writeObjectField("content", value.getContent());
            gen.writeEndObject();
        }
    }
}
