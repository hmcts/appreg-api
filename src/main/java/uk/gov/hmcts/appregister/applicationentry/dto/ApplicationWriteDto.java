package uk.gov.hmcts.appregister.applicationentry.dto;

import java.util.List;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

/** DTO for writing application data. */
public record ApplicationWriteDto(
    Long standardApplicantId,
    Long applicationCodeId,
    IdentityDetailsWriteDto applicant,
    IdentityDetailsWriteDto respondent,
    Integer numberOfBulkRespondents,
    List<String> textFields,
    String caseReference,
    String accountNumber,
    String applicationRescheduled,
    String notes,
    String bulkUpload,
    Long resultId,
    String paymentRef,
    FeeStatusType feeTypeStatus,
    Boolean includesOffsetPayment) {}
