package uk.gov.hmcts.appregister.applicationentry.mapper;

import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;

import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.Respondent;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses= {ApplicantMapper.class})
public abstract class ApplicationListEntryMapStructMapper {

    @Autowired
    ApplicantMapper aMapper;

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
     * This method checks for null values.
     *
     * @param status The application list status to covert
     * @return The converted status
     */
    public static Status toStatus(ApplicationListStatus status) {
        Status retStatus = null;
        if (status != null) {
            retStatus = Status.valueOf(status.getValue());
        }
        return retStatus;
    }

    /**
     * Convert the Status enum from the generated model to the external ApplicationListStatus enum.
     * This method checks for null values.
     *
     * @param status The status to covert
     * @return The converted application list status
     */
    public static ApplicationListStatus toStatus(Status status) {
        ApplicationListStatus retStatus = null;
        if (status != null) {
            retStatus = ApplicationListStatus.valueOf(status.getValue());
        }

        return retStatus;
    }

    @Mapping(target = "id", source = "projection.uuid")
    @Mapping(target = "applicant", expression = "java(aMapper.toApplicant(projection.getAnameaddress()))")
    @Mapping(target = "respondent", expression = "java(aMapper.toApplicant(projection.getRnameaddress()))")
    @Mapping(target = "applicationTitle", source = "projection.title")
    @Mapping(target = "isFeeRequired", expression = "java(projection.getFeeRequired().isYes())")
    @Mapping(target = "status", expression = "java(toStatus(projection.getStatus()))")
    @Mapping(target = "legislation", source = "projection.legislation")
    @Mapping(target = "isResulted", expression = "java(projection.getResult() != null)")
    public abstract EntryGetSummaryDto toEntrySummary(
            ApplicationListEntryGetSummaryProjection projection);


    /**
     * gets the entry detail dto from application list entry.
     * @param applicationListEntry The application list entry
     * @return The entry get detail dto
     */
    @Mapping(target = "id", expression = "java(applicationListEntry.getApplicationList().getUuid())")
    @Mapping(target = "listId", expression = "java(applicationListEntry.getUuid())")
    @Mapping(target = "standardApplicantCode", source = "applicationListEntry.standardApplicant.applicantCode")
    @Mapping(target = "applicationCode", source = "applicationListEntry.applicationCode.code")
    @Mapping(target = "applicant", expression = "java(aMapper.toApplicant(applicationListEntry.getAnamedaddress()))")
    @Mapping(target = "respondent", expression = "java(toRespondent(applicationListEntry.getRnameaddress()))")
    @Mapping(target = "numberOfRespondents", source="applicationListEntry.numberOfBulkRespondents")
    @Mapping(target = "wordingFields", expression = "java(getTemplateKeys(applicationListEntry.getApplicationCode()))")
    @Mapping(target = "feeStatuses", expression = "java(getFeeStatusList(statusList))")
    @Mapping(target = "hasOffsiteFee", expression = "java(fee != null && fee.isOffsite())")
    @Mapping(target = "caseReference", source = "applicationListEntry.caseReference")
    @Mapping(target = "accountNumber", source = "applicationListEntry.accountNumber")
    @Mapping(target = "notes", source = "applicationListEntry.notes")
    @Mapping(target = "officials", expression = "java(toOfficial(officials))")
    public abstract EntryGetDetailDto toEntryGetDetailDto(ApplicationListEntry applicationListEntry,
                                                          List<AppListEntryFeeStatus> statusList, Fee fee,
                                                          List<AppListEntryOfficial> officials);

    public List<Official> toOfficial(List<AppListEntryOfficial> officials) {
        List<Official> retOfficials = new ArrayList<>();
        for(AppListEntryOfficial official : officials) {
            Official off = new Official();
            off.setSurname(official.getSurname());
            off.setTitle(official.getTitle());
            off.setForename(official.getForename());
            off.setType(getOfficial(official.getOfficialType()));
            retOfficials.add(off);
        }
        return retOfficials;
    }

    /**
     * gets the working refreence strings to return
     */
    public List<String> getTemplateKeys(ApplicationCode code) {
        return WordingTemplateSentence.with(code.getWording()).getReferences();
    }

    public List<FeeStatus> getFeeStatusList(List<AppListEntryFeeStatus> feeStatusList) {
        return feeStatusList.stream()
                .map(feeStatus -> {
                    FeeStatus status = new FeeStatus();
                    status.setPaymentStatus(getStatus(feeStatus.getAlefsFeeStatus()));
                    status.setPaymentReference(feeStatus.getAlefsPaymentReference());
                    status.setStatusDate(feeStatus.getAlefsFeeStatusDate());
                    return status;
                })
                .toList();
    }

    public PaymentStatus getStatus(FeeStatusType feeStatus) {
        if (feeStatus == FeeStatusType.DUE) {
            return PaymentStatus.DUE;
        } else if (feeStatus == FeeStatusType.PAID) {
            return PaymentStatus.PAID;
        } else if (feeStatus == FeeStatusType.REMITTED) {
            return PaymentStatus.REMITTED;
        } else if (feeStatus == FeeStatusType.UNDERTAKING) {
            return PaymentStatus.UNDERTAKEN;
        }

        return null;
    }

    /**
     * A useful mapper to map the applicant details of the standard applicant.
     *
     * @param nameAddress The database name and address
     * @return The applicant Dto
     */
    /**
     * A useful mapper to map the applicant details of the standard applicant.
     *
     * @param applicant The database applicant
     * @return The applicant Dto
     */
    public Respondent toRespondent(NameAddress applicant) {

        ContactDetails contactDetails = aMapper.toContactDetails(applicant);
        Respondent respondentDto = null;
        if (applicant != null) {
            respondentDto = new Respondent();

            if (applicant.getName() != null) {
                // if the name is set then this is an organisation otherwise a person
                Organisation organisation = new Organisation();
                organisation.setName(applicant.getName());
                organisation.setContactDetails(contactDetails);
                respondentDto.setOrganisation(organisation);
            } else {
                Person person = new Person();
                FullName fullName = aMapper.toFullName(applicant);
                person.setContactDetails(contactDetails);
                person.setName(fullName);
                respondentDto.setPerson(person);
            }
        }

        return respondentDto;
    }

    public uk.gov.hmcts.appregister.generated.model.OfficialType getOfficial(OfficialType officialType) {
        return uk.gov.hmcts.appregister.generated.model.OfficialType.valueOf(officialType.getValue());
    }
}
