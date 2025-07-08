package uk.gov.hmcts.appregister.dto.write;

import java.util.List;
import uk.gov.hmcts.appregister.model.FeeStatusType;

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
