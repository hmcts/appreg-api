package uk.gov.hmcts.appregister.dto.read;

import uk.gov.hmcts.appregister.model.FeeStatusType;

import java.time.LocalDate;

public record ApplicationDto(
    Long id,
    StandardApplicantDto standardApplicant,
    ApplicationCodeDto applicationCode,
    FeeStatusType feeStatus,
    String paymentRef,
    IdentityDetailsDto applicant,
    IdentityDetailsDto respondent,
    Integer numberOfBulkRespondents,
    String applicationWording,
    String caseReference,
    String accountNumber,
    String applicationRescheduled,
    String notes,
    String bulkUpload,
    Long resultId,
    String changedBy,
    LocalDate changedDate,
    Integer version
) {}
