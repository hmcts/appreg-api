package uk.gov.hmcts.appregister.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeesReportRow {
    LocalDate listDate;
    String courthouseName;
    String otherCourthouse;
    String cjaCode;
    String standardApplicantCode;
    String applicantFullName;
    String applicationCode;
    String applicationCodeTitle;
    BigDecimal feeValue;
    BigDecimal offSiteFeeValue;
    BigDecimal totalFeeValue;
    String feeStatus;
    LocalDate feeStatusDate;
    String paymentReference;
}
