package uk.gov.hmcts.appregister.applicationentry.mapper;

import static uk.gov.hmcts.appregister.common.mapper.WordingSubstitutionKeyExtractor.getWordingKeys;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.EntityType;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.PartyType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.generated.model.RespondentPerson;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Slf4j
@Setter
public abstract class ApplicationListEntryMapper {

    @Autowired ApplicantMapper applicantMapper;

    @Autowired OfficialMapper officialMapper;

    @Autowired WordingTemplateMapper wordingTemplateMapper;

    @Mapping(
            target = "applicant",
            expression =
                    "java(org.openapitools.jackson.nullable."
                            + "JsonNullable.of("
                            + "applicantMapper"
                            + ".getNameForApplicant("
                            + "summaryProjection.getStandardApplicant(), summaryProjection.getApplicant())))")
    @Mapping(
            target = "respondent",
            expression =
                    "java(org.openapitools.jackson.nullable."
                            + "JsonNullable.of("
                            + "applicantMapper.getNameForNameAddress(summaryProjection.getRespondent())))")
    public abstract ApplicationListEntrySummary toSummaryDto(
            ApplicationListEntrySummaryProjection summaryProjection);

    public abstract List<ApplicationListEntrySummary> toSummaryDtoList(
            List<ApplicationListEntrySummaryProjection> summaryProjections);

    @Mapping(target = "id", source = "uuid")
    @Mapping(target = "applicant.person.name.title", source = "applicantTitle")
    @Mapping(target = "applicant.person.name.firstName", source = "applicantFirstName")
    @Mapping(target = "applicant.person.name.middleName", source = "applicantMiddleName")
    @Mapping(target = "applicant.person.name.lastName", source = "applicantLastName")
    @Mapping(target = "applicant.organisation.name", source = "applicantName")
    @Mapping(target = "respondent.person.name.title", source = "respondentTitle")
    @Mapping(target = "respondent.person.name.firstName", source = "respondentFirstName")
    @Mapping(target = "respondent.person.name.middleName", source = "respondentMiddleName")
    @Mapping(target = "respondent.person.name.lastName", source = "respondentLastName")
    @Mapping(target = "respondent.person.dateOfBirth", source = "respondentDateOfBirth")
    @Mapping(target = "respondent.organisation.name", source = "respondentName")
    @Mapping(target = "resultWordings", ignore = true)
    @Mapping(target = "officials", ignore = true)
    public abstract EntryGetPrintDto toPrintDto(
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
    public JsonNullable<String> map(String string) {
        return (string != null) ? JsonNullable.of(string) : JsonNullable.of(null);
    }

    /**
     * Utility mapping method to converts the given {@link OffsetDateTime} to a {@link LocalDate}.
     *
     * <p>If the input {@code offsetDateTime} is {@code null}, this method returns {@code null}.
     * Otherwise, it extracts and returns the local date component of the given {@code
     * OffsetDateTime}.
     *
     * @param offsetDateTime the OffsetDateTime to convert; may be null
     * @return the corresponding LocalDate, or null if the input is null
     */
    public LocalDate map(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toLocalDate();
    }

    public YesOrNo map(Boolean feeRequired) {
        if (feeRequired == null) {
            return null;
        }

        return feeRequired ? YesOrNo.YES : YesOrNo.NO;
    }

    public Short map(Integer sequenceNumber) {
        return sequenceNumber == null ? null : sequenceNumber.shortValue();
    }

    /**
     * Utility mapping method to maps contact details from the provided {@link
     * ApplicationListEntryPrintProjection} object to a new {@link ContactDetails} instance based on
     * the specified {@link PartyType}.
     *
     * <p>This method extracts address lines, postcode, phone, mobile, and email information for
     * either the applicant or respondent, depending on the given {@code partyType}. All fields are
     * initialized to empty strings before mapping to ensure null safety.
     *
     * @param applicationListEntryPrintProjection the projection object containing applicant and
     *     respondent contact information
     * @param partyType the type of party whose contact details should be mapped; expected values
     *     are APPLICANT or RESPONDENT
     * @return a ContactDetails object populated with the corresponding party’s contact details; if
     *     partyType is not recognized, returns an empty ContactDetails instance
     */
    public ContactDetails mapContactDetails(
            ApplicationListEntryPrintProjection applicationListEntryPrintProjection,
            PartyType partyType) {
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
            address1 = applicationListEntryPrintProjection.getApplicantAddressLine1();
            address2 = applicationListEntryPrintProjection.getApplicantAddressLine2();
            address3 = applicationListEntryPrintProjection.getApplicantAddressLine3();
            address4 = applicationListEntryPrintProjection.getApplicantAddressLine4();
            address5 = applicationListEntryPrintProjection.getApplicantAddressLine5();
            postcode = applicationListEntryPrintProjection.getApplicantPostcode();
            phone = applicationListEntryPrintProjection.getApplicantPhone();
            mobile = applicationListEntryPrintProjection.getApplicantMobile();
            email = applicationListEntryPrintProjection.getApplicantEmail();
        } else if (partyType == PartyType.RESPONDENT) {
            address1 = applicationListEntryPrintProjection.getRespondentAddressLine1();
            address2 = applicationListEntryPrintProjection.getRespondentAddressLine2();
            address3 = applicationListEntryPrintProjection.getRespondentAddressLine3();
            address4 = applicationListEntryPrintProjection.getRespondentAddressLine4();
            address5 = applicationListEntryPrintProjection.getRespondentAddressLine5();
            postcode = applicationListEntryPrintProjection.getRespondentPostcode();
            phone = applicationListEntryPrintProjection.getRespondentPhone();
            mobile = applicationListEntryPrintProjection.getRespondentMobile();
            email = applicationListEntryPrintProjection.getRespondentEmail();
        }

        details.setAddressLine1(address1);
        details.setAddressLine2(map(address2));
        details.setAddressLine3(map(address3));
        details.setAddressLine4(map(address4));
        details.setAddressLine5(map(address5));
        details.setPostcode(postcode);
        details.setPhone(map(phone));
        details.setMobile(map(mobile));
        details.setEmail(map(email));

        return details;
    }

    /**
     * Populates the {@link Applicant} and {@link Respondent} fields of the given {@link
     * EntryGetPrintDto} after the initial mapping process, ensuring all required nested objects and
     * contact details are initialized and populated.
     *
     * <p>This method is intended to be executed as an {@code @AfterMapping} hook in a MapStruct
     * mapper. It verifies that both the applicant and respondent objects exist in the target DTO,
     * and initializes any missing {@link Person} or {@link Organisation} sub-objects as required.
     * It then maps and assigns the corresponding contact details for each party, depending on
     * whether the entry represents a person or an organisation.
     *
     * @param applicationListEntryPrintProjection the source projection containing data for
     *     applicant and respondent mapping
     * @param dto the target EntryGetPrintDto object being populated after the main mapping process
     */
    @AfterMapping
    public void setApplicantAndRespondent(
            ApplicationListEntryPrintProjection applicationListEntryPrintProjection,
            @MappingTarget EntryGetPrintDto dto) {
        if (dto.getRespondent() == null) {
            dto.setRespondent(new Respondent());
        }

        EntityType applicantEntityType =
                getApplicantEntityType(applicationListEntryPrintProjection);
        EntityType respondentEntityType =
                getRespondentEntityType(applicationListEntryPrintProjection);

        if (applicantEntityType == EntityType.PERSON) {
            if (dto.getApplicant().getPerson() == null) {
                dto.getApplicant().setPerson(new Person());
            }

            dto.getApplicant()
                    .getPerson()
                    .setContactDetails(
                            mapContactDetails(
                                    applicationListEntryPrintProjection, PartyType.APPLICANT));
        }

        if (applicantEntityType == EntityType.ORGANISATION) {
            if (dto.getApplicant().getOrganisation() == null) {
                dto.getApplicant().setOrganisation(new Organisation());
            }

            dto.getApplicant()
                    .getOrganisation()
                    .setContactDetails(
                            mapContactDetails(
                                    applicationListEntryPrintProjection, PartyType.APPLICANT));
        }

        if (respondentEntityType == EntityType.PERSON) {
            if (dto.getRespondent().getPerson() == null) {
                dto.getRespondent().setPerson(new RespondentPerson());
            }

            dto.getRespondent()
                    .getPerson()
                    .setContactDetails(
                            mapContactDetails(
                                    applicationListEntryPrintProjection, PartyType.RESPONDENT));
        }

        if (respondentEntityType == EntityType.ORGANISATION) {
            if (dto.getRespondent().getOrganisation() == null) {
                dto.getRespondent().setOrganisation(new Organisation());
            }

            dto.getRespondent()
                    .getOrganisation()
                    .setContactDetails(
                            mapContactDetails(
                                    applicationListEntryPrintProjection, PartyType.RESPONDENT));
        }
    }

    private EntityType getApplicantEntityType(
            ApplicationListEntryPrintProjection applicationListEntryPrintProjection) {
        if (applicationListEntryPrintProjection.getApplicantName() != null) {
            return EntityType.ORGANISATION;
        } else if (applicationListEntryPrintProjection.getApplicantFirstName() != null
                && applicationListEntryPrintProjection.getApplicantLastName() != null) {
            return EntityType.PERSON;
        } else {
            log.warn(
                    "Unable to determine applicant entity type for application list entry ID {}: no name or"
                            + " firstName/lastName provided.",
                    applicationListEntryPrintProjection.getId());

            return EntityType.UNKNOWN;
        }
    }

    private EntityType getRespondentEntityType(
            ApplicationListEntryPrintProjection applicationListEntryPrintProjection) {
        if (applicationListEntryPrintProjection.getRespondentName() != null) {
            return EntityType.ORGANISATION;
        } else if (applicationListEntryPrintProjection.getRespondentFirstName() != null
                && applicationListEntryPrintProjection.getRespondentLastName() != null) {
            return EntityType.PERSON;
        } else {
            log.warn(
                    "Unable to determine respondent entity type for application list entry ID {}: no name or"
                            + " firstName/lastName provided.",
                    applicationListEntryPrintProjection.getId());

            return EntityType.UNKNOWN;
        }
    }

    /**
     * Convert the ApplicationListStatus enum from the generated model to the internal Status enum.
     * This method checks for null values.
     *
     * @param status The application list status to covert
     * @return The converted status
     */
    public Status toStatus(ApplicationListStatus status) {
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
    public ApplicationListStatus toStatus(Status status) {
        ApplicationListStatus retStatus = null;
        if (status != null) {
            retStatus = ApplicationListStatus.valueOf(status.getValue());
        }

        return retStatus;
    }

    @Mapping(target = "id", source = "projection.uuid")
    @Mapping(target = "applicant", expression = "java(toApplicant(projection))")
    @Mapping(target = "respondent", expression = "java(toRespondent(projection.getRnameAddress()))")
    @Mapping(target = "applicationTitle", source = "projection.title")
    @Mapping(target = "isFeeRequired", expression = "java(projection.getFeeRequired().isYes())")
    @Mapping(target = "status", expression = "java(toStatus(projection.getStatus()))")
    @Mapping(target = "legislation", source = "projection.legislation")
    @Mapping(target = "isResulted", ignore = true)
    @Mapping(target = "date", expression = "java(projection.getDateOfAl())")
    @Mapping(target = "listId", source = "projection.listId")
    @Mapping(target = "sequenceNumber", source = "projection.sequenceNumber")
    @Mapping(target = "resulted", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    public abstract EntryGetSummaryDto toEntrySummary(
            ApplicationListEntryGetSummaryProjection projection);

    public abstract ResultCodeGetSummaryDto toResultCodeGetSummaryDto(
            ResolutionCode resolutionCode);

    @AfterMapping
    protected void mapEntrySummaryAccountNumber(
            ApplicationListEntryGetSummaryProjection projection,
            @MappingTarget EntryGetSummaryDto target) {
        target.accountNumber(projection.getAccountReference());
    }

    /**
     * gets a standard applicant or a named applicant depending on which one exists.
     *
     * @param projection The projection
     * @return The applicant mapper
     */
    public Applicant toApplicant(ApplicationListEntryGetSummaryProjection projection) {
        if (projection.getAnameAddress() != null) {
            return applicantMapper.toApplicant(projection.getAnameAddress());
        } else if (projection.getStandardApplicant() != null) {
            return applicantMapper.toApplicant(
                    applicantMapper.toApplicantEntity(projection.getStandardApplicant()));
        }

        return null;
    }

    /**
     * derives an applicant from the standard applicant or a named applicant depending on which one
     * exists.
     *
     * @param applicationListEntry The app list entry
     * @param standardApplicant The standard applicant to use, can be null
     * @return The applicant mapper
     */
    public Applicant toApplicant(
            ApplicationListEntry applicationListEntry, StandardApplicant standardApplicant) {
        if (standardApplicant != null) {
            NameAddress nameAddress = applicantMapper.toApplicantEntity(standardApplicant);
            nameAddress.setCode(NameAddressCodeType.APPLICANT);
            return applicantMapper.toApplicant(nameAddress);
        }

        return applicantMapper.toApplicant(applicationListEntry.getAnamedaddress());
    }

    /**
     * gets the entry detail dto from application list entry.
     *
     * @param applicationListEntry The application list entry
     * @return The entry get detail dto
     */
    @Mapping(target = "id", expression = "java(applicationListEntry.getUuid())")
    @Mapping(
            target = "listId",
            expression = "java(applicationListEntry.getApplicationList().getUuid())")
    @Mapping(
            target = "standardApplicantCode",
            source = "applicationListEntry.standardApplicant.applicantCode")
    @Mapping(target = "applicationCode", source = "applicationListEntry.applicationCode.code")
    @Mapping(
            target = "applicant",
            expression = "java(toApplicant(applicationListEntry, applicant))")
    @Mapping(
            target = "respondent",
            expression = "java(toRespondent(applicationListEntry.getRnameaddress()))")
    @Mapping(
            target = "numberOfRespondents",
            source = "applicationListEntry.numberOfBulkRespondents")
    @Mapping(
            target = "wording",
            expression =
                    "java(wordingTemplateMapper.getTemplateDetail("
                            + "() -> applicationListEntry.getApplicationCode().getWording(),"
                            + "() -> applicationListEntry.getApplicationListEntryWording()))")
    @Mapping(target = "feeStatuses", expression = "java(getFeeStatusList(statusList))")
    @Mapping(target = "hasOffsiteFee", expression = "java(fee != null && fee.offsiteFee() != null)")
    @Mapping(target = "caseReference", source = "applicationListEntry.caseReference")
    @Mapping(target = "accountNumber", source = "applicationListEntry.accountNumber")
    @Mapping(target = "notes", source = "applicationListEntry.notes")
    @Mapping(target = "officials", expression = "java(toOfficial(officials))")
    @Mapping(target = "lodgementDate", source = "applicationListEntry.lodgementDate")
    public abstract EntryGetDetailDto toEntryGetDetailDto(
            ApplicationListEntry applicationListEntry,
            List<AppListEntryFeeStatus> statusList,
            FeePair fee,
            List<AppListEntryOfficial> officials,
            StandardApplicant applicant);

    /**
     * gets the entry detail dto from application list entry.
     *
     * @param applicationListEntry The application list entry
     * @return The entry get detail dto
     */
    @Mapping(target = "id", expression = "java(applicationListEntry.getUuid())")
    @Mapping(
            target = "listId",
            expression = "java(applicationListEntry.getApplicationList().getUuid())")
    @Mapping(
            target = "standardApplicantCode",
            source = "applicationListEntry.standardApplicant.applicantCode")
    @Mapping(target = "applicationCode", source = "applicationListEntry.applicationCode.code")
    @Mapping(
            target = "applicant",
            expression =
                    "java(toApplicant(applicationListEntry, applicationListEntry.getStandardApplicant()))")
    @Mapping(
            target = "respondent",
            expression = "java(toRespondent(applicationListEntry.getRnameaddress()))")
    @Mapping(
            target = "numberOfRespondents",
            source = "applicationListEntry.numberOfBulkRespondents")
    @Mapping(
            target = "wording",
            expression =
                    "java(wordingTemplateMapper.getTemplateDetail("
                            + "() -> applicationListEntry.getApplicationCode().getWording(),"
                            + "() -> applicationListEntry.getApplicationListEntryWording()))")
    @Mapping(
            target = "feeStatuses",
            expression = "java(getFeeStatusList(applicationListEntry.getEntryFeeStatuses()))")
    @Mapping(target = "hasOffsiteFee", source = "hasOffsiteFee")
    @Mapping(target = "caseReference", source = "applicationListEntry.caseReference")
    @Mapping(target = "accountNumber", source = "applicationListEntry.accountNumber")
    @Mapping(target = "notes", source = "applicationListEntry.notes")
    @Mapping(
            target = "officials",
            expression = "java(toOfficial(applicationListEntry.getOfficials()))")
    @Mapping(target = "lodgementDate", source = "applicationListEntry.lodgementDate")
    public abstract EntryGetDetailDto toEntryGetDetailDto(
            ApplicationListEntry applicationListEntry, boolean hasOffsiteFee);

    /**
     * converts the official entities to dto officials.
     *
     * @param officials The list of official entities
     * @return The list of officials
     */
    public List<Official> toOfficial(List<AppListEntryOfficial> officials) {
        List<Official> retOfficials = new ArrayList<>();
        for (AppListEntryOfficial official : officials) {
            Official off = new Official();
            off.setSurname(official.getSurname());
            off.setTitle(official.getTitle());
            off.setForename(official.getForename());
            off.setType(officialMapper.toOfficial(official.getOfficialType()));
            retOfficials.add(off);
        }
        return retOfficials;
    }

    /**
     * gets the working reference strings from template code.
     *
     * @param code The application code
     * @return The list of template keys (references)
     */
    public List<String> getTemplateKeys(ApplicationCode code) {
        return getWordingKeys(code.getWording());
    }

    public List<FeeStatus> getFeeStatusList(List<AppListEntryFeeStatus> feeStatusList) {
        return feeStatusList.stream()
                .map(
                        feeStatus -> {
                            FeeStatus status = new FeeStatus();
                            status.setPaymentStatus(getStatus(feeStatus.getAlefsFeeStatus()));
                            status.setPaymentReference(feeStatus.getAlefsPaymentReference());
                            status.setStatusDate(feeStatus.getAlefsFeeStatusDate());
                            return status;
                        })
                .toList();
    }

    /**
     * converts the fee status type to payment status.
     *
     * @param feeStatus The fee status type
     * @return The dto payment status
     */
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

        ContactDetails contactDetails = applicantMapper.toContactDetails(applicant);
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
                RespondentPerson person = new RespondentPerson();
                FullName fullName = applicantMapper.toFullName(applicant);
                person.setContactDetails(contactDetails);
                person.setName(fullName);
                person.setDateOfBirth(applicant.getDateOfBirth());
                respondentDto.setPerson(person);
            }
        }

        return respondentDto;
    }

    /**
     * This is used to create an audit entry using the GET request params for logging.
     *
     * @param payload Entity containing the GET request params for logging.
     * @return ApplicationListEntry Entity containing the mapped values from the GET params.
     */
    @Mapping(target = "uuid", source = "payload.entryId")
    @Mapping(target = "applicationList.uuid", source = "payload.listId")
    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicationCode", ignore = true)
    @Mapping(target = "numberOfBulkRespondents", ignore = true)
    @Mapping(target = "applicationListEntryWording", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "entryRescheduled", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "bulkUpload", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "sequenceNumber", ignore = true)
    @Mapping(target = "tcepStatus", ignore = true)
    @Mapping(target = "messageUuid", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "lodgementDate", ignore = true)
    @Mapping(target = "resolutions", ignore = true)
    @Mapping(target = "officials", ignore = true)
    @Mapping(target = "entryFeeStatuses", ignore = true)
    @Mapping(target = "entryFeeIds", ignore = true)
    @Mapping(target = "standardApplicant", ignore = true)
    @Mapping(target = "anamedaddress", ignore = true)
    @Mapping(target = "rnameaddress", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    public abstract ApplicationListEntry toApplicationListEntry(PayloadGetEntryInList payload);

    /**
     * This is used to create an audit entry using the GET dto for logging when entries are fetched
     * for a single application list.
     *
     * @param payload Entity containing the list id path parameter for logging.
     * @param filterDto Entity containing the GET query params for logging.
     * @return ApplicationListEntry Entity containing the mapped values from the GET params.
     */
    @Mapping(target = "applicationList.uuid", source = "payload.listId")
    @Mapping(target = "anamedaddress.name", source = "filterDto.applicantName")
    @Mapping(target = "rnameaddress.name", source = "filterDto.respondentName")
    @Mapping(target = "rnameaddress.postcode", source = "filterDto.respondentPostcode")
    @Mapping(target = "accountNumber", source = "filterDto.accountReference")
    @Mapping(target = "applicationCode.title", source = "filterDto.applicationTitle")
    @Mapping(target = "applicationCode.feeDue", source = "filterDto.feeRequired")
    @Mapping(target = "sequenceNumber", source = "filterDto.sequenceNumber")
    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicationCode.code", ignore = true)
    @Mapping(target = "applicationCode.wording", ignore = true)
    @Mapping(target = "applicationCode.legislation", ignore = true)
    @Mapping(target = "applicationCode.requiresRespondent", ignore = true)
    @Mapping(target = "applicationCode.destinationEmail1", ignore = true)
    @Mapping(target = "applicationCode.destinationEmail2", ignore = true)
    @Mapping(target = "applicationCode.startDate", ignore = true)
    @Mapping(target = "applicationCode.endDate", ignore = true)
    @Mapping(target = "applicationCode.bulkRespondentAllowed", ignore = true)
    @Mapping(target = "applicationCode.version", ignore = true)
    @Mapping(target = "applicationCode.userName", ignore = true)
    @Mapping(target = "applicationCode.feeReference", ignore = true)
    @Mapping(target = "applicationCode.applicationListEntryList", ignore = true)
    @Mapping(target = "numberOfBulkRespondents", ignore = true)
    @Mapping(target = "applicationListEntryWording", ignore = true)
    @Mapping(target = "entryRescheduled", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "bulkUpload", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "tcepStatus", ignore = true)
    @Mapping(target = "messageUuid", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "lodgementDate", ignore = true)
    @Mapping(target = "resolutions", ignore = true)
    @Mapping(target = "officials", ignore = true)
    @Mapping(target = "entryFeeStatuses", ignore = true)
    @Mapping(target = "entryFeeIds", ignore = true)
    @Mapping(target = "standardApplicant", ignore = true)
    @Mapping(target = "anamedaddress.code", ignore = true)
    @Mapping(target = "anamedaddress.id", ignore = true)
    @Mapping(target = "anamedaddress.firstName", ignore = true)
    @Mapping(target = "anamedaddress.middleName", ignore = true)
    @Mapping(target = "anamedaddress.lastName", ignore = true)
    @Mapping(target = "anamedaddress.title", ignore = true)
    @Mapping(target = "anamedaddress.address1", ignore = true)
    @Mapping(target = "anamedaddress.address2", ignore = true)
    @Mapping(target = "anamedaddress.address3", ignore = true)
    @Mapping(target = "anamedaddress.address4", ignore = true)
    @Mapping(target = "anamedaddress.address5", ignore = true)
    @Mapping(target = "anamedaddress.emailAddress", ignore = true)
    @Mapping(target = "anamedaddress.telephoneNumber", ignore = true)
    @Mapping(target = "anamedaddress.mobileNumber", ignore = true)
    @Mapping(target = "anamedaddress.version", ignore = true)
    @Mapping(target = "anamedaddress.userName", ignore = true)
    @Mapping(target = "anamedaddress.dateOfBirth", ignore = true)
    @Mapping(target = "anamedaddress.dmsId", ignore = true)
    @Mapping(target = "rnameaddress.code", ignore = true)
    @Mapping(target = "rnameaddress.id", ignore = true)
    @Mapping(target = "rnameaddress.firstName", ignore = true)
    @Mapping(target = "rnameaddress.middleName", ignore = true)
    @Mapping(target = "rnameaddress.lastName", ignore = true)
    @Mapping(target = "rnameaddress.title", ignore = true)
    @Mapping(target = "rnameaddress.address1", ignore = true)
    @Mapping(target = "rnameaddress.address2", ignore = true)
    @Mapping(target = "rnameaddress.address3", ignore = true)
    @Mapping(target = "rnameaddress.address4", ignore = true)
    @Mapping(target = "rnameaddress.address5", ignore = true)
    @Mapping(target = "rnameaddress.emailAddress", ignore = true)
    @Mapping(target = "rnameaddress.telephoneNumber", ignore = true)
    @Mapping(target = "rnameaddress.mobileNumber", ignore = true)
    @Mapping(target = "rnameaddress.version", ignore = true)
    @Mapping(target = "rnameaddress.userName", ignore = true)
    @Mapping(target = "rnameaddress.dateOfBirth", ignore = true)
    @Mapping(target = "rnameaddress.dmsId", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    public abstract ApplicationListEntry toApplicationListEntry(
            PayloadGetEntryInList payload, EntryApplicationListGetFilterDto filterDto);

    /**
     * This is used to create an audit entry using the GET dto for logging.
     *
     * @param filterDto Entity containing the GET dto for logging.
     * @return ApplicationListEntry Entity containing the mapped values from the GET params.
     */
    @Mapping(target = "accountNumber", source = "filterDto.accountReference")
    @Mapping(target = "standardApplicant.applicantCode", source = "filterDto.standardApplicantCode")
    @Mapping(target = "anamedaddress.name", source = "filterDto.applicantOrganisation")
    @Mapping(target = "anamedaddress.lastName", source = "filterDto.applicantSurname")
    @Mapping(target = "rnameaddress.name", source = "filterDto.respondentOrganisation")
    @Mapping(target = "rnameaddress.lastName", source = "filterDto.respondentSurname")
    @Mapping(target = "rnameaddress.postcode", source = "filterDto.respondentPostcode")
    @Mapping(target = "applicationList.courtCode", source = "filterDto.courtCode")
    @Mapping(
            target = "applicationList.otherLocation",
            source = "filterDto.otherLocationDescription")
    @Mapping(target = "applicationList.date", source = "filterDto.date")
    @Mapping(target = "applicationList.cja.code", source = "filterDto.cjaCode")
    @Mapping(
            target = "applicationList.status",
            expression = "java(toStatus(entryGetFilterDto.getStatus()))")
    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicationCode.title", source = "filterDto.applicationTitle")
    @Mapping(target = "applicationCode", ignore = true)
    @Mapping(target = "numberOfBulkRespondents", ignore = true)
    @Mapping(target = "applicationListEntryWording", ignore = true)
    @Mapping(target = "entryRescheduled", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "bulkUpload", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "sequenceNumber", ignore = true)
    @Mapping(target = "tcepStatus", ignore = true)
    @Mapping(target = "messageUuid", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "lodgementDate", ignore = true)
    @Mapping(target = "resolutions", ignore = true)
    @Mapping(target = "officials", ignore = true)
    @Mapping(target = "entryFeeStatuses", ignore = true)
    @Mapping(target = "entryFeeIds", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    public abstract ApplicationListEntry toApplicationListEntry(EntryGetFilterDto filterDto);

    @Mapping(target = "respondent", ignore = true) // handled in AfterMapping
    @Mapping(target = "applicant", ignore = true) // we use applicantCode instead
    @Mapping(target = "standardApplicantCode", source = "applicantCode")
    @Mapping(target = "wordingFields", ignore = true)
    @Mapping(target = "feeStatuses", ignore = true)
    @Mapping(target = "officials", ignore = true)
    @Mapping(target = "numberOfRespondents", ignore = true)
    @Mapping(target = "hasOffsiteFee", constant = "false")
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "lodgementDate", ignore = true)
    public abstract EntryCreateDto toEntryCreateDto(BulkUploadRow row);

    @AfterMapping
    protected void mapBulkUploadFields(BulkUploadRow row, @MappingTarget EntryCreateDto dto) {
        // --- Respondent ---
        dto.setRespondent(toBulkUploadRespondent(row));

        // --- Wording fields ---
        List<String> applicationTextValues = row.getApplicationTextValues();
        if (!applicationTextValues.isEmpty()) {
            List<TemplateSubstitution> substitutions =
                    getTemplateSubstitutions(applicationTextValues);

            dto.setWordingFields(substitutions);
        }

        truncateBulkUploadEntryCreateDto(dto);
    }

    private static @NonNull List<TemplateSubstitution> getTemplateSubstitutions(
            List<String> applicationTextValues) {
        List<TemplateSubstitution> substitutions = new ArrayList<>();

        for (String applicationTextValue : applicationTextValues) {
            TemplateSubstitution substitution = new TemplateSubstitution();
            substitution.setValue(applicationTextValue);
            substitutions.add(substitution);
        }

        return substitutions;
    }

    private ContactDetails toRespondentContactDetails(BulkUploadRow row) {
        ContactDetails contactDetails = new ContactDetails();

        contactDetails.setAddressLine1(row.getRespondentAddressLine1());
        contactDetails.setAddressLine2(map(row.getRespondentAddressLine2()));
        contactDetails.setAddressLine3(map(row.getRespondentAddressLine3()));
        contactDetails.setAddressLine4(map(row.getRespondentAddressLine4()));
        contactDetails.setAddressLine5(map(row.getRespondentAddressLine5()));
        contactDetails.setPostcode(row.getRespondentPostcode());
        contactDetails.setEmail(map(row.getRespondentEmail()));
        contactDetails.setPhone(map(row.getRespondentTelephone()));
        contactDetails.setMobile(map(row.getRespondentMobile()));

        return contactDetails;
    }

    private Respondent toBulkUploadRespondent(BulkUploadRow row) {
        Respondent respondent = new Respondent();

        ContactDetails contactDetails = toRespondentContactDetails(row);

        if (StringUtils.isNotBlank(row.getRespondentOrganisationName())) {

            Organisation organisation = new Organisation();
            organisation.setName(row.getRespondentOrganisationName());
            organisation.setContactDetails(contactDetails);

            respondent.setOrganisation(organisation);
        } else {
            FullName name = new FullName();
            name.setTitle(row.getRespondentTitle());
            name.setFirstName(row.getRespondentFirstNameValue());
            name.setMiddleName(map(row.getRespondentMiddleNameValue()));
            name.setLastName(row.getRespondentLastNameValue());

            RespondentPerson person = new RespondentPerson();
            person.setName(name);
            person.setContactDetails(contactDetails);

            respondent.setPerson(person);
        }

        return respondent;
    }

    private void truncateBulkUploadEntryCreateDto(EntryCreateDto dto) {
        if (dto == null) {
            return;
        }

        dto.setStandardApplicantCode(StringUtils.left(dto.getStandardApplicantCode(), 10));
        dto.setApplicationCode(StringUtils.left(dto.getApplicationCode(), 10));
        dto.setAccountNumber(StringUtils.left(dto.getAccountNumber(), 20));
        dto.setCaseReference(StringUtils.left(dto.getCaseReference(), 15));
        dto.setNotes(StringUtils.left(dto.getNotes(), 4000));

        truncateApplicant(dto.getApplicant());
        truncateRespondent(dto.getRespondent());
    }

    private void truncateApplicant(Applicant applicant) {
        if (applicant == null) {
            return;
        }

        if (applicant.getOrganisation() != null) {
            Organisation organisation = applicant.getOrganisation();
            organisation.setName(StringUtils.left(organisation.getName(), 100));
            truncateContactDetails(organisation.getContactDetails());
        }

        if (applicant.getPerson() != null) {
            truncateFullName(applicant.getPerson().getName());
            truncateContactDetails(applicant.getPerson().getContactDetails());
        }
    }

    private void truncateRespondent(Respondent respondent) {
        if (respondent == null) {
            return;
        }

        if (respondent.getOrganisation() != null) {
            Organisation organisation = respondent.getOrganisation();
            organisation.setName(StringUtils.left(organisation.getName(), 100));
            truncateContactDetails(organisation.getContactDetails());
        }

        if (respondent.getPerson() != null) {
            RespondentPerson person = respondent.getPerson();
            truncateFullName(person.getName());
            truncateContactDetails(person.getContactDetails());
        }
    }

    private void truncateFullName(FullName name) {
        if (name == null) {
            return;
        }

        name.setTitle(StringUtils.left(name.getTitle(), 100));
        name.setFirstName(StringUtils.left(name.getFirstName(), 100));
        name.setMiddleName(truncate(name.getMiddleName(), 100));
        name.setLastName(StringUtils.left(name.getLastName(), 100));
    }

    private void truncateContactDetails(ContactDetails details) {
        if (details == null) {
            return;
        }

        details.setAddressLine1(StringUtils.left(details.getAddressLine1(), 35));
        details.setAddressLine2(truncate(details.getAddressLine2(), 35));
        details.setAddressLine3(truncate(details.getAddressLine3(), 35));
        details.setAddressLine4(truncate(details.getAddressLine4(), 35));
        details.setAddressLine5(truncate(details.getAddressLine5(), 35));
        details.setPostcode(StringUtils.left(details.getPostcode(), 8));
        details.setEmail(truncate(details.getEmail(), 253));
        details.setPhone(truncate(details.getPhone(), 20));
        details.setMobile(truncate(details.getMobile(), 20));
    }

    private JsonNullable<String> truncate(JsonNullable<String> value, int length) {
        if (value == null || !value.isPresent()) {
            return value;
        }

        return JsonNullable.of(StringUtils.left(value.get(), length));
    }
}
