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
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;

/**
 * Utility class for obfuscating sensitive data in logs. This is used to prevent sensitive data from
 * being logged in plain text, which can be a security risk.
 */
@Slf4j
public class ObfuscationUtil {
    private static final String REDACTED = "[REDACTED]";
    private static final String ACCOUNT_NUMBER_FIELD = "accountNumber";

    static final ObjectMapper mapper = createObjectMapper();

    private ObfuscationUtil() {
        // Utility class
    }

    private static ObjectMapper createObjectMapper() {
        SimpleModule maskingModule = new SimpleModule();

        maskingModule.addSerializer(Person.class, new RedactedSerializer<>());
        maskingModule.addSerializer(Organisation.class, new RedactedSerializer<>());
        maskingModule.addSerializer(NameAddress.class, new RedactedSerializer<>());
        maskingModule.addSerializer(EntryGetDetailDto.class, new RedactedSerializer<>());
        maskingModule.addSerializer(EntryGetPrintDto.class, new RedactedSerializer<>());
        maskingModule.addSerializer(EntryCreateDto.class, new RedactedSerializer<>());
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
        maskingModule.addSerializer(FeesReportFilterDto.class, new FeesReportFilterDtoSensitiveSerializer());
        maskingModule.addSerializer(PrivateProsecutorsIndexFilterDto.class, new PrivateProsecutorIndexSensitiveSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setConfig(
                objectMapper
                        .getSerializationConfig()
                        .with(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_OPTIONALS));
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.registerModule(maskingModule);
        objectMapper.registerModule(new JsonNullableModule());
        objectMapper.registerModule(new JavaTimeModule());

        return objectMapper;
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
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error(jsonProcessingException.getMessage(), jsonProcessingException);
            return "Can't obfuscate object";
        }
    }

    static class RedactedSerializer<T> extends JsonSerializer<T> {

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
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
                gen.writeStringField(ACCOUNT_NUMBER_FIELD, REDACTED);
            } else {
                gen.writeNullField(ACCOUNT_NUMBER_FIELD);
            }

            gen.writeEndObject();
        }
    }

    /** Serializer to redact EntryPage PII data. */
    static class EntryPageSensitiveSerializer extends JsonSerializer<EntryPage> {

        @Override
        public void serialize(EntryPage value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            writePage(
                    gen,
                    value.getPageNumber(),
                    value.getPageSize(),
                    value.getTotalElements(),
                    value.getTotalPages(),
                    value.getSort(),
                    value.getFirst(),
                    value.getLast(),
                    value.getElementsOnPage(),
                    value.getContent());
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

            gen.writeStringField(ACCOUNT_NUMBER_FIELD, REDACTED);
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

    static class FeesReportFilterDtoSensitiveSerializer extends JsonSerializer<FeesReportFilterDto> {

        @Override
        public void serialize(
                FeesReportFilterDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("dateFrom", value.getDateFrom());
            gen.writeObjectField("dateTo", value.getDateTo());
            gen.writeStringField("standardApplicantCode", REDACTED);
            gen.writeStringField("applicantName", REDACTED);
            gen.writeObjectField("location", value.getLocation());
            gen.writeEndObject();
        }
    }

    static class PrivateProsecutorIndexSensitiveSerializer extends JsonSerializer<PrivateProsecutorsIndexFilterDto> {

        @Override
        public void serialize(
                PrivateProsecutorsIndexFilterDto value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeStringField("applicantFirstName", REDACTED);
            gen.writeStringField("applicantSurname", REDACTED);
            gen.writeStringField("applicantOrganisationName", REDACTED);
            gen.writeStringField("respondentSurname", REDACTED);
            gen.writeStringField("respondentFirstname", REDACTED);
            gen.writeStringField("respondentOrganisationName", REDACTED);
            gen.writeStringField("standardApplicantName", REDACTED);
            gen.writeObjectField("dateFrom", value.getDateFrom());
            gen.writeObjectField("dateTo", value.getDateTo());
            gen.writeEndObject();
        }
    }

    static class ResultPageSensitiveSerializer extends JsonSerializer<ResultPage> {

        @Override
        public void serialize(ResultPage value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            writePage(
                    gen,
                    value.getPageNumber(),
                    value.getPageSize(),
                    value.getTotalElements(),
                    value.getTotalPages(),
                    value.getSort(),
                    value.getFirst(),
                    value.getLast(),
                    value.getElementsOnPage(),
                    value.getContent());
        }
    }

    private static void writePage(
            JsonGenerator gen,
            Object pageNumber,
            Object pageSize,
            Object totalElements,
            Object totalPages,
            Object sort,
            Object first,
            Object last,
            Object elementsOnPage,
            Object content)
            throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("pageNumber", pageNumber);
        gen.writeObjectField("pageSize", pageSize);
        gen.writeObjectField("totalElements", totalElements);
        gen.writeObjectField("totalPages", totalPages);
        gen.writeObjectField("sort", sort);
        gen.writeObjectField("first", first);
        gen.writeObjectField("last", last);
        gen.writeObjectField("elementsOnPage", elementsOnPage);
        gen.writeObjectField("content", content);
        gen.writeEndObject();
    }
}
