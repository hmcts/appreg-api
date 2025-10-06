package uk.gov.hmcts.appregister.applicationentry.dto;

import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

public record ApplicationListEntryDto(
        Long id,
        StandardApplicantDto standardApplicant,
        ApplicationCodeDto applicationCode,
        FeeStatusType feeStatus,
        String paymentRef,
        IdentityDetailsDto applicant,
        IdentityDetailsDto respondent,
        Short numberOfBulkRespondents,
        String applicationWording,
        String caseReference,
        String accountNumber,
        String applicationRescheduled,
        String notes,
        String bulkUpload,
        Long resultId,
        String changedBy,
        OffsetDateTime changedDate,
        Long version) {}
