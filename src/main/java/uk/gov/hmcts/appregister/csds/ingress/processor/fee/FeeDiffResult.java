package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import java.util.List;
import java.util.Map;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

public record FeeDiffResult(
        Map<Long, FeeIngressRecord> incomingById,
        Map<Long, FeeIngressRecord> existingById,
        List<IngressDiffRecord<FeeIngressRecord, FeeIngressRecord, FeeIngressRecord>>
                diffRecords) {}
