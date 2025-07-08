package uk.gov.hmcts.appregister.dto.read;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.model.FeeStatusType;

public record ApplicationFeeRecordDto(
        Long id,
        String paymentReference,
        FeeStatusType feeStatus,
        LocalDate statusDate,
        OffsetDateTime creationDate,
        BigDecimal amount,
        String feeDescription) {}
