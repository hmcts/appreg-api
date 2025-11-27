package uk.gov.hmcts.appregister.applicationentry.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.Respondent;

/**
 * Maps ApplicationListEntry related entities to associated entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListEntryEntityMapper {

    @Autowired
    OfficialMapper officialMapper;

    @Mapping(target = "title", source = "person.name.title")
    @Mapping(target = "surname", source = "person.name.surname")
    @Mapping(target = "forename1", source = "person.name.firstForename")
    @Mapping(target = "forename2", source = "person.name.secondForename")
    @Mapping(target = "forename3", source = "person.name.thirdForename")
    @Mapping(target = "address1", source = "person.contactDetails.addressLine1")
    @Mapping(target = "address2", source = "person.contactDetails.addressLine2")
    @Mapping(target = "address3", source = "person.contactDetails.addressLine3")
    @Mapping(target = "address4", source = "person.contactDetails.addressLine4")
    @Mapping(target = "address5", source = "person.contactDetails.addressLine5")
    @Mapping(target = "postcode", source = "person.contactDetails.postcode")
    @Mapping(target = "telephoneNumber", source = "person.contactDetails.phone")
    @Mapping(target = "mobileNumber", source = "person.contactDetails.mobile")
    @Mapping(target = "emailAddress", source = "person.contactDetails.email")
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "name", ignore = true)
    abstract NameAddress toPerson(Person person);

    @Mapping(target = "name", source = "organisation.name")
    @Mapping(target = "address1", source = "organisation.contactDetails.addressLine1")
    @Mapping(target = "address2", source = "organisation.contactDetails.addressLine2")
    @Mapping(target = "address3", source = "organisation.contactDetails.addressLine3")
    @Mapping(target = "address4", source = "organisation.contactDetails.addressLine4")
    @Mapping(target = "address5", source = "organisation.contactDetails.addressLine5")
    @Mapping(target = "postcode", source = "organisation.contactDetails.postcode")
    @Mapping(target = "telephoneNumber", source = "organisation.contactDetails.phone")
    @Mapping(target = "mobileNumber", source = "organisation.contactDetails.mobile")
    @Mapping(target = "emailAddress", source = "organisation.contactDetails.email")
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "surname", ignore = true)
    @Mapping(target = "forename1", ignore = true)
    @Mapping(target = "forename2", ignore = true)
    @Mapping(target = "forename3", ignore = true)
    abstract NameAddress toOrganisation(Organisation organisation);

    @Mapping(target = "applicationListEntryWording", source = "substituteWording")
    @Mapping(target = "applicationCode", source = "code")
    @Mapping(target = "standardApplicant", source = "standardApplicant")
    @Mapping(target = "anamedaddress", source = "applicant")
    @Mapping(target = "rnameaddress", source = "respondent")
    @Mapping(target = "accountNumber", source = "entryCreateDto.accountNumber")
    @Mapping(target = "caseReference", source = "entryCreateDto.caseReference")
    @Mapping(target = "lodgementDate", source = "entryCreateDto.lodgementDate")
    @Mapping(target = "notes", source = "entryCreateDto.notes")
    @Mapping(target = "applicationList", source = "applicationList")
    @Mapping(target = "numberOfBulkRespondents", source = "entryCreateDto.numberOfRespondents")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "bulkUpload", ignore = true)
    @Mapping(target = "tcepStatus", ignore = true)
    @Mapping(target = "messageUuid", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "resolutions", ignore = true)
    @Mapping(target = "entryFeeIds", ignore = true)
    @Mapping(target = "entryFeeStatuses", ignore = true)
    @Mapping(target = "officials", ignore = true)
    @Mapping(target = "entryRescheduled", constant = "N")
    @Mapping(target = "sequenceNumber", constant = "1")
    public abstract ApplicationListEntry toApplicationListEntry(
            EntryCreateDto entryCreateDto,
            String substituteWording,
            StandardApplicant standardApplicant,
            NameAddress applicant,
            NameAddress respondent,
            ApplicationCode code,
            ApplicationList applicationList);

    public NameAddress toApplicantNameAddress(Applicant applicant) {
        if (applicant.getPerson() != null) {
            return toPerson(applicant.getPerson());
        } else if (applicant.getOrganisation() != null) {
            return toOrganisation(applicant.getOrganisation());
        } else {
            return null;
        }
    }

    public NameAddress toRespondentNameAddress(Respondent applicant) {
        if (applicant.getPerson() != null) {
            return toPerson(applicant.getPerson());
        } else if (applicant.getOrganisation() != null) {
            return toOrganisation(applicant.getOrganisation());
        } else {
            return null;
        }
    }

    /**
     * Maps the applicant to a name address
     *
     * @param applicant The applicant details
     * @return The mapped entity
     */
    public NameAddress toApplicant(Applicant applicant) {
        NameAddress nameAddress = toApplicantNameAddress(applicant);
        nameAddress.setCode(NameAddress.APPLICANT_CODE);
        return nameAddress;
    }

    /**
     * Maps the respondent to a name address
     *
     * @param respondent The respondent details
     * @return The mapped entity
     */
    public NameAddress toRespondent(Respondent respondent) {
        NameAddress nameAddress = toRespondentNameAddress(respondent);
        nameAddress.setCode(NameAddress.RESPONDENT_CODE);
        return nameAddress;
    }

    @Mapping(
            target = "alefsFeeStatus",
            expression = "java(toStatus(feeStatus.getPaymentStatus()))")
    @Mapping(target = "appListEntry", source = "applicationListEntry")
    @Mapping(target = "alefsFeeStatusDate", source = "feeStatus.statusDate")
    @Mapping(target = "alefsPaymentReference", source = "feeStatus.paymentReference")
    @Mapping(target = "alefsStatusCreationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract AppListEntryFeeStatus toFeeStatus(
            FeeStatus feeStatus, ApplicationListEntry applicationListEntry);

    public FeeStatusType toStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == PaymentStatus.DUE) {
            return FeeStatusType.DUE;
        } else if (paymentStatus == PaymentStatus.PAID) {
            return FeeStatusType.PAID;
        } else if (paymentStatus == PaymentStatus.REMITTED) {
            return FeeStatusType.REMITTED;
        } else if (paymentStatus == PaymentStatus.UNDERTAKEN) {
            return FeeStatusType.UNDERTAKING;
        }

        return null;
    }

    @Mapping(target = "appListEntry", source = "listEntryEntity")
    @Mapping(target = "forename", source = "official.forename")
    @Mapping(target = "surname", source = "official.surname")
    @Mapping(target = "officialType", expression = "java(officialMapper.toOfficial(official.getType()))")
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract AppListEntryOfficial toOfficial(
            Official official, ApplicationListEntry listEntryEntity);

}
