package uk.gov.hmcts.appregister.applicationentry.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListEntryMapStructMapper {

    public abstract ApplicationListEntrySummary toSummaryDto(
            ApplicationListEntrySummaryProjection summaryProjection);

    public abstract List<ApplicationListEntrySummary> toSummaryDtoList(
            List<ApplicationListEntrySummaryProjection> summaryProjections);

    /**
     * Utility mapping method to wrap a {@link String} in a {@link JsonNullable}.
     *
     * <p>This allows optional String fields (e.g. {@code accountNumber}) to be properly represented
     * in generated OpenAPI models where {@code null} and "undefined" must be distinguished.
     *
     * @param string the String value
     * @return a JsonNullable wrapper containing the value or null
     */
    public JsonNullable<String> map(String string) {
        return (string != null) ? JsonNullable.of(string) : JsonNullable.of(null);
    }

    /**
     * Convert the ApplicationListStatus enum from the generated model to the internal Status enum.
     * @return The status
     */
    public Status toStatus(ApplicationListStatus status) {
        return Status.valueOf(status.getValue());
    }

    /**
     * Convert the ApplicationListStatus enum from the generated model to the internal Status enum.
     * @return The status
     */
    public ApplicationListStatus toStatus(Status status) {
        return ApplicationListStatus.valueOf(status.getValue());
    }

    @Mapping(target = "id", source = "applicationListEntry.uuid")
    @Mapping(target = "applicantName", expression = "java(toApplicantName(applicationListEntry))")
    @Mapping(target = "respondentName", expression = "java(toRespondentName(applicationListEntry))")
    @Mapping(
            target = "applicationTitle",
            source = "applicationListEntry.applicationList.description")
    @Mapping(
            target = "isFeeRequired",
            expression = "java(toFee(applicationListEntry.getApplicationCode().getFeeDue()))")
    @Mapping(target = "isResulted", expression = "java(toResulted(applicationListEntry))")
    @Mapping(target = "status", source = "applicationListEntry.applicationList.status")
    abstract EntryGetSummaryDto toEntrySummary(ApplicationListEntry applicationListEntry);

    public EntryGetSummaryDto toApplicationListGetSummaryDto(ApplicationListEntryGetSummaryProjection projection) {
        EntryGetSummaryDto entryGetSummaryDto = new EntryGetSummaryDto();
        entryGetSummaryDto.setApplicant(toApplicant(projection.getAnamedaddress()));
        entryGetSummaryDto.setRespondent(toApplicant(projection.getRspondentAddress()));
        entryGetSummaryDto.setStatus(toStatus(projection.getStatus()));
        entryGetSummaryDto.setApplicationTitle(projection.getApplicationTitle());
        entryGetSummaryDto.setId(UUID.fromString(projection.getUuid()));
        entryGetSummaryDto.setIsFeeRequired(projection.isFreeRequired());
        entryGetSummaryDto.setIsResulted(projection.getResult() != null);
        entryGetSummaryDto.setLegislation(projection.getLegislation());
        return null;
    }

    /**
     * A useful mapper to map the applicant details of the standard applicant.
     *
     * @param applicant The database applicant
     * @return The applicant Dto
     */
    public Applicant toApplicant(NameAddress applicant) {
        Applicant applicantDto = new Applicant();

        ContactDetails contactDetails = toContactDetails(applicant);

        if (applicant.getName() != null) {
           // if the name is set then this is an organisation otherwise a person
            Organisation organisation = new Organisation();
            organisation.setName(applicant.getName());
            organisation.setContactDetails(contactDetails);
            applicantDto.setOrganisation(organisation);
        } else {
            Person person = new Person();
            FullName fullName = toFullName(applicant);
            person.setContactDetails(contactDetails);
            person.setName(fullName);
            applicantDto.setPerson(person);
        }

        return applicantDto;
    }

    /**
     * to full name.
     *
     * @param applicant The standard applicant
     * @return The full name
     */
    FullName toFullName(NameAddress applicant) {
        FullName fullName = new FullName();
        fullName.setTitle(applicant.getTitle());
        fullName.setFirstForename(applicant.getForename1());
        fullName.setSecondForename(applicant.getForename2());
        fullName.setThirdForename(applicant.getForename3());
        fullName.setSurname(applicant.getSurname());
        return fullName;
    }

    /**
     * to contact details.
     *
     * @param applicant The standard applicant
     * @return The contact details
     */
    ContactDetails toContactDetails(NameAddress applicant) {
        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setAddressLine1(applicant.getAddress1());
        contactDetails.setAddressLine2(applicant.getAddress2());
        contactDetails.setAddressLine3(applicant.getAddress3());
        contactDetails.setAddressLine4(applicant.getAddress4());
        contactDetails.setAddressLine5(applicant.getAddress5());
        contactDetails.setEmail(applicant.getEmailAddress());
        contactDetails.setMobile(applicant.getMobileNumber());
        contactDetails.setPhone(applicant.getTelephoneNumber());
        contactDetails.setPostcode(applicant.getPostcode());
        return contactDetails;
    }
}
