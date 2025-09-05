package uk.gov.hmcts.appregister.applicationentry.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

/** DTO for application fee record. */
public record ApplicationFeeRecordDto(
        Long id,
        String paymentReference,
        FeeStatusType feeStatus,
        LocalDate statusDate,
        OffsetDateTime creationDate,
        BigDecimal amount,
        String feeDescription) {}
