package uk.gov.hmcts.appregister.applicationentry.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;

import uk.gov.hmcts.appregister.common.enumeration.PartyType;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ApplicationListEntryMapStructMapper {

    ApplicationListEntrySummary toSummaryDto(
            ApplicationListEntrySummaryProjection summaryProjection);

    List<ApplicationListEntrySummary> toSummaryDtoList(
            List<ApplicationListEntrySummaryProjection> summaryProjections);

    @Mapping(target = "applicant.person.name.title", source = "applicantTitle")
    @Mapping(target = "applicant.person.name.surname", source = "applicantSurname")
    @Mapping(target = "applicant.person.name.firstForename", source = "applicantForename1")
    @Mapping(target = "applicant.person.name.secondForename", source = "applicantForename2")
    @Mapping(target = "applicant.person.name.thirdForename", source = "applicantForename3")
    @Mapping(target = "applicant.organisation.name", source = "applicantName")
    @Mapping(target = "respondent.person.name.title", source = "respondentTitle")
    @Mapping(target = "respondent.person.name.surname", source = "respondentSurname")
    @Mapping(target = "respondent.person.name.firstForename", source = "respondentForename1")
    @Mapping(target = "respondent.person.name.secondForename", source = "respondentForename2")
    @Mapping(target = "respondent.person.name.thirdForename", source = "respondentForename3")
    @Mapping(target = "respondent.organisation.name", source = "respondentName")
    EntryGetPrintDto toPrintDto(
        ApplicationListEntryPrintProjection printProjection);

    /**
     * Utility mapping method to wrap a {@link String} in a {@link JsonNullable}.
     *
     * <p>This allows optional String fields (e.g. {@code accountNumber}) to be properly represented
     * in generated OpenAPI models where {@code null} and "undefined" must be distinguished.
     *
     * @param string the String value
     * @return a JsonNullable wrapper containing the value or null
     */
    default JsonNullable<String> map(String string) {
        return (string != null) ? JsonNullable.of(string) : JsonNullable.of(null);
    }

    default ContactDetails mapContactDetails(
        ApplicationListEntryPrintProjection applicationListEntryPrintProjection, PartyType partyType) {
        ContactDetails details = new ContactDetails();

        String address1 = "";
        String address2 = "";
        String address3 = "";
        String address4 = "";
        String address5 = "";
        String postcode = "";
        String phone = "";
        String mobile = "";
        String email = "";

        if (partyType == PartyType.APPLICANT) {
            address1 = applicationListEntryPrintProjection.getApplicantAddress1();
            address2 = applicationListEntryPrintProjection.getApplicantAddress2();
            address3 = applicationListEntryPrintProjection.getApplicantAddress3();
            address4 = applicationListEntryPrintProjection.getApplicantAddress4();
            address5 = applicationListEntryPrintProjection.getApplicantAddress5();
            postcode = applicationListEntryPrintProjection.getApplicantPostcode();
            phone = applicationListEntryPrintProjection.getApplicantPhone();
            mobile = applicationListEntryPrintProjection.getApplicantMobile();
            email = applicationListEntryPrintProjection.getApplicantEmail();
        }  else if (partyType == PartyType.RESPONDENT) {
            address1 = applicationListEntryPrintProjection.getRespondentAddress1();
            address2 = applicationListEntryPrintProjection.getRespondentAddress2();
            address3 = applicationListEntryPrintProjection.getRespondentAddress3();
            address4 = applicationListEntryPrintProjection.getRespondentAddress4();
            address5 = applicationListEntryPrintProjection.getRespondentAddress5();
            postcode = applicationListEntryPrintProjection.getRespondentPostcode();
            phone = applicationListEntryPrintProjection.getRespondentPhone();
            mobile = applicationListEntryPrintProjection.getRespondentMobile();
            email = applicationListEntryPrintProjection.getRespondentEmail();
        }

        details.setAddressLine1(address1);
        details.setAddressLine2(address2);
        details.setAddressLine3(address3);
        details.setAddressLine4(address4);
        details.setAddressLine5(address5);
        details.setPostcode(postcode);
        details.setPhone(phone);
        details.setMobile(mobile);
        details.setEmail(email);

        return details;
    }

    default EntryGetPrintDto setApplicantContactDetailsByType(
        ApplicationListEntryPrintProjection applicationListEntryPrintProjection) {
        EntryGetPrintDto dto = new EntryGetPrintDto();

        if (isPerson(applicationListEntryPrintProjection)) {
            if (dto.getApplicant() != null && dto.getApplicant().getPerson() != null) {
                dto.getApplicant().getPerson().setContactDetails(mapContactDetails(
                    applicationListEntryPrintProjection, PartyType.APPLICANT));
            }

            if (dto.getRespondent() != null && dto.getRespondent().getPerson() != null) {
                dto.getRespondent().getPerson().setContactDetails(mapContactDetails(
                    applicationListEntryPrintProjection, PartyType.RESPONDENT));
            }
        } else {
            if (dto.getApplicant() != null && dto.getApplicant().getOrganisation() != null) {
                dto.getApplicant().getOrganisation().setContactDetails(mapContactDetails(
                    applicationListEntryPrintProjection,  PartyType.APPLICANT));
            }

            if (dto.getRespondent() != null && dto.getRespondent().getOrganisation() != null) {
                dto.getRespondent().getOrganisation().setContactDetails(mapContactDetails(
                    applicationListEntryPrintProjection,   PartyType.RESPONDENT));
            }
        }

        return dto;
    }

    private boolean isPerson(ApplicationListEntryPrintProjection applicationListEntryPrintProjection) {
        if (applicationListEntryPrintProjection == null) {
            return false;
        } else {
            return applicationListEntryPrintProjection.getApplicantTitle() != null ||
                applicationListEntryPrintProjection.getApplicantSurname() != null ||
                applicationListEntryPrintProjection.getApplicantForename1() != null ||
                applicationListEntryPrintProjection.getRespondentTitle() != null ||
                applicationListEntryPrintProjection.getRespondentSurname() != null ||
                applicationListEntryPrintProjection.getRespondentForename1() != null;
        }
    }
}
