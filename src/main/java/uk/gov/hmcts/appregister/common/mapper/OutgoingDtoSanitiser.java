package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.generated.model.RespondentPerson;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

public final class OutgoingDtoSanitiser {
    private OutgoingDtoSanitiser() {
        // Utility class.
    }

    public static String emptyToNull(String value) {
        return "".equals(value) ? null : value;
    }

    public static JsonNullable<String> emptyToNull(JsonNullable<String> value) {
        if (value != null && value.isPresent() && "".equals(value.orElse(null))) {
            return JsonNullable.of(null);
        }
        return value;
    }

    public static Applicant sanitize(Applicant applicant) {
        if (applicant == null) {
            return null;
        }
        sanitize(applicant.getOrganisation());
        sanitize(applicant.getPerson());
        return applicant;
    }

    public static Respondent sanitize(Respondent respondent) {
        if (respondent == null) {
            return null;
        }
        sanitize(respondent.getOrganisation());
        sanitize(respondent.getPerson());
        return respondent;
    }

    public static Person sanitize(Person person) {
        if (person == null) {
            return null;
        }
        sanitize(person.getName());
        sanitize(person.getContactDetails());
        return person;
    }

    public static RespondentPerson sanitize(RespondentPerson person) {
        if (person == null) {
            return null;
        }
        sanitize(person.getName());
        sanitize(person.getContactDetails());
        return person;
    }

    public static Organisation sanitize(Organisation organisation) {
        if (organisation == null) {
            return null;
        }
        organisation.setName(emptyToNull(organisation.getName()));
        sanitize(organisation.getContactDetails());
        return organisation;
    }

    public static FullName sanitize(FullName fullName) {
        if (fullName == null) {
            return null;
        }
        fullName.setTitle(emptyToNull(fullName.getTitle()));
        fullName.setFirstName(emptyToNull(fullName.getFirstName()));
        fullName.setMiddleName(emptyToNull(fullName.getMiddleName()));
        fullName.setLastName(emptyToNull(fullName.getLastName()));
        return fullName;
    }

    public static ContactDetails sanitize(ContactDetails details) {
        if (details == null) {
            return null;
        }
        details.setAddressLine1(emptyToNull(details.getAddressLine1()));
        details.setAddressLine2(emptyToNull(details.getAddressLine2()));
        details.setAddressLine3(emptyToNull(details.getAddressLine3()));
        details.setAddressLine4(emptyToNull(details.getAddressLine4()));
        details.setAddressLine5(emptyToNull(details.getAddressLine5()));
        details.setPostcode(emptyToNull(details.getPostcode()));
        details.setPhone(emptyToNull(details.getPhone()));
        details.setMobile(emptyToNull(details.getMobile()));
        details.setEmail(emptyToNull(details.getEmail()));
        return details;
    }

    public static Official sanitize(Official official) {
        if (official == null) {
            return null;
        }
        official.setTitle(emptyToNull(official.getTitle()));
        official.setForename(emptyToNull(official.getForename()));
        official.setSurname(emptyToNull(official.getSurname()));
        return official;
    }

    public static FeeStatus sanitize(FeeStatus status) {
        if (status == null) {
            return null;
        }
        status.setPaymentReference(emptyToNull(status.getPaymentReference()));
        return status;
    }

    public static TemplateDetail sanitize(TemplateDetail detail) {
        if (detail == null) {
            return null;
        }
        detail.setTemplate(emptyToNull(detail.getTemplate()));
        if (detail.getSubstitutionKeyConstraints() != null) {
            detail.getSubstitutionKeyConstraints().forEach(OutgoingDtoSanitiser::sanitize);
        }
        return detail;
    }

    public static TemplateKeyWithConstraint sanitize(TemplateKeyWithConstraint detail) {
        if (detail == null) {
            return null;
        }
        detail.setKey(emptyToNull(detail.getKey()));
        detail.setValue(emptyToNull(detail.getValue()));
        return detail;
    }

    public static TemplateSubstitution sanitize(TemplateSubstitution substitution) {
        if (substitution == null) {
            return null;
        }
        substitution.setKey(emptyToNull(substitution.getKey()));
        substitution.setValue(emptyToNull(substitution.getValue()));
        return substitution;
    }

    public static EntryGetSummaryDto sanitize(EntryGetSummaryDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setApplicationTitle(emptyToNull(dto.getApplicationTitle()));
        dto.setLegislation(emptyToNull(dto.getLegislation()));
        dto.setAccountNumber(emptyToNull(dto.getAccountNumber()));
        sanitize(dto.getApplicant());
        sanitize(dto.getRespondent());
        if (dto.getResulted() != null) {
            dto.getResulted().forEach(OutgoingDtoSanitiser::sanitize);
        }
        return dto;
    }

    public static EntryGetDetailDto sanitize(EntryGetDetailDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setStandardApplicantCode(emptyToNull(dto.getStandardApplicantCode()));
        dto.setApplicationCode(emptyToNull(dto.getApplicationCode()));
        dto.setCaseReference(emptyToNull(dto.getCaseReference()));
        dto.setAccountNumber(emptyToNull(dto.getAccountNumber()));
        dto.setNotes(emptyToNull(dto.getNotes()));
        sanitize(dto.getApplicant());
        sanitize(dto.getRespondent());
        sanitize(dto.getWording());
        if (dto.getOfficials() != null) {
            dto.getOfficials().forEach(OutgoingDtoSanitiser::sanitize);
        }
        if (dto.getFeeStatuses() != null) {
            dto.getFeeStatuses().forEach(OutgoingDtoSanitiser::sanitize);
        }
        return dto;
    }

    public static EntryGetPrintDto sanitize(EntryGetPrintDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setApplicationCode(emptyToNull(dto.getApplicationCode()));
        dto.setApplicationTitle(emptyToNull(dto.getApplicationTitle()));
        dto.setApplicationWording(emptyToNull(dto.getApplicationWording()));
        dto.setCaseReference(emptyToNull(dto.getCaseReference()));
        dto.setAccountReference(emptyToNull(dto.getAccountReference()));
        dto.setNotes(emptyToNull(dto.getNotes()));
        sanitize(dto.getApplicant());
        sanitize(dto.getRespondent());
        sanitizeStringList(dto.getResultWordings());
        if (dto.getOfficials() != null) {
            dto.getOfficials().forEach(OutgoingDtoSanitiser::sanitize);
        }
        return dto;
    }

    public static ResultGetDto sanitize(ResultGetDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setResultCode(emptyToNull(dto.getResultCode()));
        sanitize(dto.getWording());
        return dto;
    }

    public static ApplicationListGetSummaryDto sanitize(ApplicationListGetSummaryDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setLocation(emptyToNull(dto.getLocation()));
        dto.setDescription(emptyToNull(dto.getDescription()));
        return dto;
    }

    public static ApplicationListGetDetailDto sanitize(ApplicationListGetDetailDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setDescription(emptyToNull(dto.getDescription()));
        dto.setCjaCode(emptyToNull(dto.getCjaCode()));
        dto.setCourtCode(emptyToNull(dto.getCourtCode()));
        dto.setCourtName(emptyToNull(dto.getCourtName()));
        dto.setOtherLocationDescription(emptyToNull(dto.getOtherLocationDescription()));
        return dto;
    }

    public static ApplicationListGetPrintDto sanitize(ApplicationListGetPrintDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setCourtName(emptyToNull(dto.getCourtName()));
        dto.setOtherLocationDescription(emptyToNull(dto.getOtherLocationDescription()));
        dto.setDuration(emptyToNull(dto.getDuration()));
        dto.setCja(emptyToNull(dto.getCja()));
        Optional.ofNullable(dto.getEntries())
                .ifPresent(entries -> entries.forEach(OutgoingDtoSanitiser::sanitize));
        return dto;
    }

    public static ApplicationCodeGetSummaryDto sanitize(ApplicationCodeGetSummaryDto dto) {
        if (dto == null) {
            return null;
        }
        return sanitizeApplicationCode(
                dto,
                ApplicationCodeGetSummaryDto::getApplicationCode,
                ApplicationCodeGetSummaryDto::setApplicationCode,
                ApplicationCodeGetSummaryDto::getTitle,
                ApplicationCodeGetSummaryDto::setTitle,
                ApplicationCodeGetSummaryDto::getFeeReference,
                ApplicationCodeGetSummaryDto::setFeeReference,
                ApplicationCodeGetSummaryDto::getFeeDescription,
                ApplicationCodeGetSummaryDto::setFeeDescription,
                ApplicationCodeGetSummaryDto::getOffsiteFeeReference,
                ApplicationCodeGetSummaryDto::setOffsiteFeeReference,
                ApplicationCodeGetSummaryDto::getOffsiteFeeDescription,
                ApplicationCodeGetSummaryDto::setOffsiteFeeDescription,
                ApplicationCodeGetSummaryDto::getWording);
    }

    public static ApplicationCodeGetDetailDto sanitize(ApplicationCodeGetDetailDto dto) {
        if (dto == null) {
            return null;
        }
        return sanitizeApplicationCode(
                dto,
                ApplicationCodeGetDetailDto::getApplicationCode,
                ApplicationCodeGetDetailDto::setApplicationCode,
                ApplicationCodeGetDetailDto::getTitle,
                ApplicationCodeGetDetailDto::setTitle,
                ApplicationCodeGetDetailDto::getFeeReference,
                ApplicationCodeGetDetailDto::setFeeReference,
                ApplicationCodeGetDetailDto::getFeeDescription,
                ApplicationCodeGetDetailDto::setFeeDescription,
                ApplicationCodeGetDetailDto::getOffsiteFeeReference,
                ApplicationCodeGetDetailDto::setOffsiteFeeReference,
                ApplicationCodeGetDetailDto::getOffsiteFeeDescription,
                ApplicationCodeGetDetailDto::setOffsiteFeeDescription,
                ApplicationCodeGetDetailDto::getWording);
    }

    public static ResultCodeGetSummaryDto sanitize(ResultCodeGetSummaryDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setResultCode(emptyToNull(dto.getResultCode()));
        dto.setTitle(emptyToNull(dto.getTitle()));
        return dto;
    }

    public static ResultCodeGetDetailDto sanitize(ResultCodeGetDetailDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setResultCode(emptyToNull(dto.getResultCode()));
        dto.setTitle(emptyToNull(dto.getTitle()));
        sanitize(dto.getWording());
        return dto;
    }

    public static CourtLocationGetSummaryDto sanitize(CourtLocationGetSummaryDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setName(emptyToNull(dto.getName()));
        dto.setLocationCode(emptyToNull(dto.getLocationCode()));
        return dto;
    }

    public static CourtLocationGetDetailDto sanitize(CourtLocationGetDetailDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setName(emptyToNull(dto.getName()));
        dto.setLocationCode(emptyToNull(dto.getLocationCode()));
        return dto;
    }

    public static CriminalJusticeAreaGetDto sanitize(CriminalJusticeAreaGetDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setCode(emptyToNull(dto.getCode()));
        dto.setDescription(emptyToNull(dto.getDescription()));
        return dto;
    }

    public static StandardApplicantGetSummaryDto sanitize(StandardApplicantGetSummaryDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setCode(emptyToNull(dto.getCode()));
        sanitize(dto.getApplicant());
        return dto;
    }

    public static StandardApplicantGetDetailDto sanitize(StandardApplicantGetDetailDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setCode(emptyToNull(dto.getCode()));
        sanitize(dto.getApplicant());
        return dto;
    }

    public static JobAcknowledgement sanitize(JobAcknowledgement dto) {
        if (dto == null) {
            return null;
        }
        dto.setErrorDescription(emptyToNull(dto.getErrorDescription()));
        return dto;
    }

    @SuppressWarnings("java:S107")
    private static <T> T sanitizeApplicationCode(
            T dto,
            Function<T, String> applicationCodeGetter,
            BiConsumer<T, String> applicationCodeSetter,
            Function<T, String> titleGetter,
            BiConsumer<T, String> titleSetter,
            Function<T, JsonNullable<String>> feeReferenceGetter,
            BiConsumer<T, JsonNullable<String>> feeReferenceSetter,
            Function<T, JsonNullable<String>> feeDescriptionGetter,
            BiConsumer<T, JsonNullable<String>> feeDescriptionSetter,
            Function<T, JsonNullable<String>> offsiteFeeReferenceGetter,
            BiConsumer<T, JsonNullable<String>> offsiteFeeReferenceSetter,
            Function<T, JsonNullable<String>> offsiteFeeDescriptionGetter,
            BiConsumer<T, JsonNullable<String>> offsiteFeeDescriptionSetter,
            Function<T, TemplateDetail> wordingGetter) {
        sanitizeStringField(dto, applicationCodeGetter, applicationCodeSetter);
        sanitizeStringField(dto, titleGetter, titleSetter);
        sanitizeNullableStringField(dto, feeReferenceGetter, feeReferenceSetter);
        sanitizeNullableStringField(dto, feeDescriptionGetter, feeDescriptionSetter);
        sanitizeNullableStringField(dto, offsiteFeeReferenceGetter, offsiteFeeReferenceSetter);
        sanitizeNullableStringField(dto, offsiteFeeDescriptionGetter, offsiteFeeDescriptionSetter);
        sanitize(wordingGetter.apply(dto));
        return dto;
    }

    private static <T> void sanitizeStringField(
            T dto, Function<T, String> getter, BiConsumer<T, String> setter) {
        setter.accept(dto, emptyToNull(getter.apply(dto)));
    }

    private static <T> void sanitizeNullableStringField(
            T dto,
            Function<T, JsonNullable<String>> getter,
            BiConsumer<T, JsonNullable<String>> setter) {
        setter.accept(dto, emptyToNull(getter.apply(dto)));
    }

    private static void sanitizeStringList(List<String> values) {
        if (values != null) {
            values.replaceAll(OutgoingDtoSanitiser::emptyToNull);
        }
    }
}
