package uk.gov.hmcts.appregister.applicationentry.dto;

import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

/** DTO for application fee record. */
public record AppListEntryFeeStatusDto(
        Long id,
        String paymentReference,
        FeeStatusType feeStatus,
        OffsetDateTime statusDate,
        OffsetDateTime creationDate,
        Double amount,
        String feeDescription) {}
