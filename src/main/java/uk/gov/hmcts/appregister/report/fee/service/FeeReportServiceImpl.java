package uk.gov.hmcts.appregister.report.fee.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.report.fee.dto.FeeReportFilterDto;
import uk.gov.hmcts.appregister.report.fee.dto.FeeReportRowDto;
import uk.gov.hmcts.appregister.report.fee.repository.FeeReportJdbcRepository;
import uk.gov.hmcts.appregister.report.shared.CsvReportGenerator;

/** Service implementation for generating fee reports. */
@Service
@RequiredArgsConstructor
public class FeeReportServiceImpl implements FeeReportService {

    private final FeeReportJdbcRepository feeReportRepository;
    private final CsvReportGenerator csvReportGenerator;

    @Override
    public void generateFeeReportCsv(FeeReportFilterDto filter, HttpServletResponse response)
            throws IOException {
        String wrappedStandardApplicantCode = filter.standardApplicantCode();
        String wrappedApplicantSurname = filter.applicantSurname();
        String wrappedCourthouseCode = filter.courthouseCode();

        List<FeeReportRowDto> feeReport =
                feeReportRepository.generateFeeReport(
                        filter.startDate(),
                        filter.endDate(),
                        wrappedStandardApplicantCode,
                        wrappedApplicantSurname,
                        wrappedCourthouseCode);

        if (feeReport.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        csvReportGenerator.writeFeeReport(feeReport, response);
    }
}
