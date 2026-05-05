package uk.gov.hmcts.appregister.report.audit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReportJobAuditTest {

    @Test
    void givenReportJobAudit_whenGettingId_thenReturnsNullToMatchReportParameterAudit() {
        Assertions.assertNull(ReportJobAudit.builder().build().getId());
    }
}
