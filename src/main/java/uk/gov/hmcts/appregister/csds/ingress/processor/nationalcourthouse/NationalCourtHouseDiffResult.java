package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import java.util.List;
import java.util.Map;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

public record NationalCourtHouseDiffResult(
        Map<Long, NationalCourtHouseIngressRecord> incomingById,
        Map<Long, NationalCourtHouseIngressRecord> existingById,
        List<
                        IngressDiffRecord<
                                NationalCourtHouseIngressRecord,
                                NationalCourtHouseIngressRecord,
                                NationalCourtHouseIngressRecord>>
                diffRecords) {}
