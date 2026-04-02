package uk.gov.hmcts.appregister.report.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import uk.gov.hmcts.appregister.generated.api.ReportsApi;

import java.util.UUID;

public class ReportController implements ReportsApi {
    @Override
    public ResponseEntity<Resource> downloadReport(UUID jobId) {
        return ReportsApi.super.downloadReport(jobId);
    }
}
