package uk.gov.hmcts.appregister.report.fee.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.report.fee.dto.FeeReportFilterDto;
import uk.gov.hmcts.appregister.report.fee.service.FeeReportService;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class FeeReportController {

    private final FeeReportService feeReportService;

    @GetMapping(value = "/fees", produces = "text/csv")
    public void downloadFeeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String standardApplicantCode,
            @RequestParam(required = false) String applicantSurname,
            @RequestParam(required = false) String courthouseCode,
            HttpServletResponse response)
            throws IOException {
        FeeReportFilterDto filter =
                new FeeReportFilterDto(
                        startDate,
                        endDate,
                        standardApplicantCode,
                        applicantSurname,
                        courthouseCode);
        feeReportService.generateFeeReportCsv(filter, response);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee-report.csv");
    }
}
