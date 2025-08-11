package uk.gov.hmcts.appregister.applicationentry.dto;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationentry.model.FeeStatusType;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

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
        Integer version) {}
