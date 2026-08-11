package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;

class FeesReportLifecycle extends ReportCsvLifecycle<FeesReportRow> {
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

    FeesReportLifecycle() throws IOException {
        super("fees-report", "Fees Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(FeesReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            CsvUtil.escapeCharacters(Objects.toString(row.getCourthouseName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getOtherCourthouse(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getCjaCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getStandardApplicantCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicantFullName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicationCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicationCodeTitle(), "")),
            formatMoney(row.getFeeValue()),
            formatMoney(row.getOffSiteFeeValue()),
            formatMoney(row.getTotalFeeValue()),
            CsvUtil.escapeCharacters(Objects.toString(row.getFeeStatus(), "")),
            formatIsoDate(row.getFeeStatusDate()),
            CsvUtil.escapeCharacters(Objects.toString(row.getPaymentReference(), ""))
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
