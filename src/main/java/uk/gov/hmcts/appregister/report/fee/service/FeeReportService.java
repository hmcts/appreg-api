package uk.gov.hmcts.appregister.report.fee.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import uk.gov.hmcts.appregister.report.fee.dto.FeeReportFilterDto;

/** Service interface for generating fee reports. */
public interface FeeReportService {
    void generateFeeReportCsv(FeeReportFilterDto filter, HttpServletResponse response)
            throws IOException;
}
