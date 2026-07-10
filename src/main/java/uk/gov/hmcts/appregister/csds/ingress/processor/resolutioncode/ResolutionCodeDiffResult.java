package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import java.util.List;
import java.util.Map;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

public record ResolutionCodeDiffResult(
        Map<Long, ResolutionCodeIngressRecord> incomingById,
        Map<Long, ResolutionCodeIngressRecord> existingById,
        List<
                        IngressDiffRecord<
                                ResolutionCodeIngressRecord,
                                ResolutionCodeIngressRecord,
                                ResolutionCodeIngressRecord>>
                diffRecords) {}
