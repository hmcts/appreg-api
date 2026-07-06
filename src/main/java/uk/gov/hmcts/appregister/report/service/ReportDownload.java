package uk.gov.hmcts.appregister.report.service;

import org.springframework.core.io.InputStreamResource;

public record ReportDownload(String filename, InputStreamResource resource) {}
