package uk.gov.hmcts.appregister.report.service;

import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

public record ReportJobCreation(JobAcknowledgement acknowledgement, Auditable reportParameters) {}
