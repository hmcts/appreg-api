package uk.gov.hmcts.appregister.report.job;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;

public class FeesReportLifecycle extends ReportCsvLifecycle<FeesReportRow> {
    private static final DateTimeFormatter LIST_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
        "List Date",
        "List Court House Name",
        "List Other Location",
        "CJA Code",
        "Standard Applicant Code",
        "Applicant Name/Surname",
        "Application Code",
        "Application Code Title",
        "Fee Value",
        "Off Site Fee Value",
        "Total fee Value",
        "Fee Status",
        "Fee Status Date",
        "Payment Reference"
    };

    public FeesReportLifecycle() throws IOException {
        super("fees-report", "Fees Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(FeesReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            Objects.toString(row.getCourthouseName(), ""),
            Objects.toString(row.getOtherCourthouse(), ""),
            Objects.toString(row.getCjaCode(), ""),
            Objects.toString(row.getStandardApplicantCode(), ""),
            Objects.toString(row.getApplicantFullName(), ""),
            Objects.toString(row.getApplicationCode(), ""),
            Objects.toString(row.getApplicationCodeTitle(), ""),
            formatMoney(row.getFeeValue()),
            formatMoney(row.getOffSiteFeeValue()),
            formatMoney(row.getTotalFeeValue()),
            Objects.toString(row.getFeeStatus(), ""),
            formatIsoDate(row.getFeeStatusDate()),
            Objects.toString(row.getPaymentReference(), "")
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }

    private String formatIsoDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
