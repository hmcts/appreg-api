package uk.gov.hmcts.appregister.applicationentry.mapper;

import static uk.gov.hmcts.appregister.common.mapper.WordingSubstitutionKeyExtractor.getWordingKeys;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.EntityType;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.PartyType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
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
        } else if (applicationListEntryPrintProjection.getApplicantForename1() != null
                && applicationListEntryPrintProjection.getApplicantSurname() != null) {
            return EntityType.PERSON;
        } else {
            log.warn(
                    "Unable to determine applicant entity type for application list entry ID {}: no name or"
                            + "forename/surname provided.",
                    applicationListEntryPrintProjection.getId());

            return EntityType.UNKNOWN;
        }
    }

    private EntityType getRespondentEntityType(
            ApplicationListEntryPrintProjection applicationListEntryPrintProjection) {
        if (applicationListEntryPrintProjection.getRespondentName() != null) {
            return EntityType.ORGANISATION;
        } else if (applicationListEntryPrintProjection.getRespondentForename1() != null
                && applicationListEntryPrintProjection.getRespondentSurname() != null) {
            return EntityType.PERSON;
        } else {
            log.warn(
                    "Unable to determine respondent entity type for application list entry ID {}: no name or"
                            + "forename/surname provided.",
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
    @Mapping(
            target = "respondent",
            expression = "java(applicantMapper.toApplicant(projection.getRnameAddress()))")
    @Mapping(target = "applicationTitle", source = "projection.title")
    @Mapping(target = "isFeeRequired", expression = "java(projection.getFeeRequired().isYes())")
    @Mapping(target = "status", expression = "java(toStatus(projection.getStatus()))")
    @Mapping(target = "legislation", source = "projection.legislation")
    @Mapping(target = "isResulted", expression = "java(projection.getResult() != null)")
    @Mapping(target = "date", expression = "java(projection.getDateOfAl())")
    @Mapping(target = "listId", source = "projection.listId")
    @Mapping(target = "sequenceNumber", source = "projection.sequenceNumber")
    @Mapping(target = "resulted", source = "resolutionCode")
    @Mapping(target = "accountNumber", source = "accountReference")
    public abstract EntryGetSummaryDto toEntrySummary(
            ApplicationListEntryGetSummaryProjection projection);

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
    @Mapping(target = "hasOffsiteFee", expression = "java(fee != null && fee.isOffsite())")
    @Mapping(target = "caseReference", source = "applicationListEntry.caseReference")
    @Mapping(target = "accountNumber", source = "applicationListEntry.accountNumber")
    @Mapping(target = "notes", source = "applicationListEntry.notes")
    @Mapping(target = "officials", expression = "java(toOfficial(officials))")
    @Mapping(target = "lodgementDate", source = "applicationListEntry.lodgementDate")
    public abstract EntryGetDetailDto toEntryGetDetailDto(
            ApplicationListEntry applicationListEntry,
            List<AppListEntryFeeStatus> statusList,
            Fee fee,
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
     * This is used to create an audit entry using the GET dto for logging.
     *
     * @param filterDto Entity containing the GET dto for logging.
     * @return ApplicationListEntry Entity containing the mapped values from the GET params.
     */
    @Mapping(target = "accountNumber", source = "filterDto.accountReference")
    @Mapping(target = "standardApplicant.applicantCode", source = "filterDto.standardApplicantCode")
    @Mapping(target = "anamedaddress.name", source = "filterDto.applicantOrganisation")
    @Mapping(target = "anamedaddress.surname", source = "filterDto.applicantSurname")
    @Mapping(target = "rnameaddress.name", source = "filterDto.respondentOrganisation")
    @Mapping(target = "rnameaddress.surname", source = "filterDto.respondentSurname")
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
}
