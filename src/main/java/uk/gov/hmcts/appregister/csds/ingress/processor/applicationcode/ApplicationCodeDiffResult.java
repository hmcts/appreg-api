package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import java.util.List;
import java.util.Map;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

public record ApplicationCodeDiffResult(
        Map<Long, ApplicationCodeIngressRecord> incomingById,
        Map<Long, ApplicationCodeIngressRecord> existingById,
        List<
                        IngressDiffRecord<
                                ApplicationCodeIngressRecord,
                                ApplicationCodeIngressRecord,
                                ApplicationCodeIngressRecord>>
                diffRecords) {}
