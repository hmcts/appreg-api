package uk.gov.hmcts.appregister.applicationentryresult.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

class BulkCreateApplicationEntryResultAuditTest {

    @Test
    void formatHelpers_renderExpectedEscapedValues() {
        var entry = new ApplicationListEntry();
        entry.setId(22L);
        entry.setUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        entry.setSequenceNumber((short) 7);

        var resolutionCode = new ResolutionCode();
        resolutionCode.setResultCode("CODE\\\"1");

        var resolution = new AppListEntryResolution();
        resolution.setId(11L);
        resolution.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        resolution.setApplicationList(entry);
        resolution.setResolutionCode(resolutionCode);
        resolution.setResolutionWording("Wording\\\"1");
        resolution.setResolutionOfficer("Officer\\\"1");

        assertThat(
                        BulkCreateApplicationEntryResultAudit.formatWordingFields(
                                List.of(new TemplateSubstitution().key("k\\\"1").value("v\\\"1"))))
                .isEqualTo("[{\"key\":\"k\\\\\\\"1\",\"value\":\"v\\\\\\\"1\"}]");
        assertThat(BulkCreateApplicationEntryResultAudit.formatCreatedResults(List.of(resolution)))
                .isEqualTo(
                        "[{\"resultId\":\"11111111-1111-1111-1111-111111111111\","
                                + "\"entryId\":\"22222222-2222-2222-2222-222222222222\","
                                + "\"sequenceNumber\":7,\n"
                                + "\"resultCode\":\"CODE\\\\\\\"1\","
                                + "\"wording\":\"Wording\\\\\\\"1\","
                                + "\"officer\":\"Officer\\\\\\\"1\"}]");
    }
}
