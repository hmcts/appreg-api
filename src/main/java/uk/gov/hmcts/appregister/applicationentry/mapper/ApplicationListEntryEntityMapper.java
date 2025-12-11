package uk.gov.hmcts.appregister.applicationentry.mapper;

import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;

/**
 * Maps ApplicationListEntry related entities to associated entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Setter
public abstract class ApplicationListEntryEntityMapper {

    @Autowired OfficialMapper officialMapper;

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
    @Mapping(target = "uuid", ignore = true)
    public abstract ApplicationListEntry toApplicationListEntry(
            EntryCreateDto entryCreateDto,
            String substituteWording,
            StandardApplicant standardApplicant,
            NameAddress applicant,
            NameAddress respondent,
            ApplicationCode code,
            ApplicationList applicationList);

    @Mapping(target = "applicationListEntryWording", source = "substituteWording")
    @Mapping(target = "applicationCode", source = "code")
    @Mapping(target = "standardApplicant", source = "standardApplicant")
    @Mapping(target = "anamedaddress", ignore = true)
    @Mapping(target = "rnameaddress", ignore = true)
    @Mapping(target = "accountNumber", source = "entryUpdateDto.accountNumber")
    @Mapping(target = "caseReference", source = "entryUpdateDto.caseReference")
    @Mapping(target = "lodgementDate", source = "entryUpdateDto.lodgementDate")
    @Mapping(target = "notes", source = "entryUpdateDto.notes")
    @Mapping(target = "applicationList", source = "applicationList")
    @Mapping(target = "numberOfBulkRespondents", source = "entryUpdateDto.numberOfRespondents")
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
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "changedBy", ignore = true)
    @Mapping(target = "changedDate", ignore = true)
    public abstract void toApplicationListEntry(
            EntryUpdateDto entryUpdateDto,
            String substituteWording,
            StandardApplicant standardApplicant,
            ApplicationCode code,
            ApplicationList applicationList,
            @MappingTarget ApplicationListEntry entry);

    @Mapping(target = "alefsFeeStatus", expression = "java(toStatus(feeStatus.getPaymentStatus()))")
    @Mapping(target = "appListEntry", source = "applicationListEntry")
    @Mapping(target = "alefsFeeStatusDate", source = "feeStatus.statusDate")
    @Mapping(target = "alefsPaymentReference", source = "feeStatus.paymentReference")
    @Mapping(target = "alefsStatusCreationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract AppListEntryFeeStatus toFeeStatus(
            FeeStatus feeStatus, ApplicationListEntry applicationListEntry);

    /**
     * Converts the payment status to fee status type.
     *
     * @param paymentStatus The payment status
     * @return The fee status type
     */
    public static FeeStatusType toStatus(PaymentStatus paymentStatus) {
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
    @Mapping(
            target = "officialType",
            expression = "java(officialMapper.toOfficial(official.getType()))")
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract AppListEntryOfficial toOfficial(
            Official official, ApplicationListEntry listEntryEntity);
}
