package uk.gov.hmcts.appregister.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.read.FeeReportRowDto;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvReportGenerator {

    private static final String HEADER = String.join(",",
                                                     "List Date",
                                                     "List Court House Name",
                                                     "List Other Location",
                                                     "Standard Applicant Code",
                                                     "Applicant Name/Surname",
                                                     "Application Code",
                                                     "Application Code Title"
    );

    public void writeFeeReport(List<FeeReportRowDto> rows, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv");

        try (OutputStream out = response.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {

            // Write BOM for Excel compatibility
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            writer.println(HEADER);

            for (FeeReportRowDto row : rows) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                              formatCsvField(row.listDate().toString()),
                              formatCsvField(row.courthouseName()),
                              formatCsvField(row.otherCourthouse()),
                              formatCsvField(row.standardApplicantCode()),
                              formatCsvField(row.applicantNameOrSurname()),
                              formatCsvField(row.applicationCode()),
                              formatCsvField(row.applicationCodeTitle())
                );
            }
        }
    }

    private String formatCsvField(String value) {
        if (value == null) return "";
        // Prevent Excel from interpreting as formula
        if (value.matches("^[=+\\-@].*")) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}


