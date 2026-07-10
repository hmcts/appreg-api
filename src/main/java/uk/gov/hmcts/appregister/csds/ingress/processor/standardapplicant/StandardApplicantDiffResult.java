package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.util.List;
import java.util.Map;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

public record StandardApplicantDiffResult(
        Map<Long, StandardApplicantIngressRecord> incomingById,
        Map<Long, StandardApplicantIngressRecord> existingById,
        List<
                        IngressDiffRecord<
                                StandardApplicantIngressRecord,
                                StandardApplicantIngressRecord,
                                StandardApplicantIngressRecord>>
                diffRecords) {}
