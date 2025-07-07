package uk.gov.hmcts.appregister.dto.read;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplicationCodeDto(
    Long id,
    String applicationCode,
    String title,
    String wording,
    String legislation,
    Boolean feeDue,
    Boolean requiresRespondent,
    String destinationEmail1,
    String destinationEmail2,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Boolean bulkRespondentAllowed,
    String feeReference,
    String mainFeeDescription,
    BigDecimal mainFeeAmount,
    String offsetFeeDescription,
    BigDecimal offsetFeeAmount
) {}
