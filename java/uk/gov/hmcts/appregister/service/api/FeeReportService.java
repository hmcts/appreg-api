package uk.gov.hmcts.appregister.service.api;

import jakarta.servlet.http.HttpServletResponse;
import uk.gov.hmcts.appregister.dto.read.FeeReportFilterDto;

import java.io.IOException;

public interface FeeReportService {
    void generateFeeReportCsv(FeeReportFilterDto filter, HttpServletResponse response) throws IOException;
}
