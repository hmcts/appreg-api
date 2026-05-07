package uk.gov.hmcts.appregister.applicationentryresult.audit;

import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

class ApplicationListEntryResultAuditTest {

    @Test
    void extractAuditData_createAddsJoinColumnRows() {
        val audit = ApplicationListEntryResultAudit.from(createResolution());

        // Convert the emitted audit rows into a simple lookup so the expected columns are easy to
        // assert one-by-one.
        val auditRowsByColumn =
                audit.extractAuditData(CrudEnum.CREATE).stream()
                        .collect(
                                Collectors.toMap(
                                        AuditableData::getFieldName,
                                        Function.identity(),
                                        (left, right) -> right));

        Assertions.assertEquals("11", auditRowsByColumn.get("aler_id").getValue());
        Assertions.assertEquals("22", auditRowsByColumn.get("ale_ale_id").getValue());
        Assertions.assertEquals("33", auditRowsByColumn.get("rc_rc_id").getValue());
        Assertions.assertEquals(
                "Updated wording", auditRowsByColumn.get("al_entry_resolution_wording").getValue());
        Assertions.assertEquals(
                "Officer", auditRowsByColumn.get("al_entry_resolution_officer").getValue());
        Assertions.assertEquals("4", auditRowsByColumn.get("version").getValue());
    }

    @Test
    void extractAuditData_updateAddsResolutionCodeJoinButNotEntryJoin() {
        val audit = ApplicationListEntryResultAudit.from(createResolution());

        // Update audit should capture the changed result-code foreign key, but not repeat the
        // owning entry id because ale_ale_id is not an update field in the ticket contract.
        val auditRowsByColumn =
                audit.extractAuditData(CrudEnum.UPDATE).stream()
                        .collect(
                                Collectors.toMap(
                                        AuditableData::getFieldName,
                                        Function.identity(),
                                        (left, right) -> right));

        Assertions.assertFalse(auditRowsByColumn.containsKey("ale_ale_id"));
        Assertions.assertEquals("33", auditRowsByColumn.get("rc_rc_id").getValue());
        Assertions.assertEquals(
                "Updated wording", auditRowsByColumn.get("al_entry_resolution_wording").getValue());
        Assertions.assertEquals(
                "Officer", auditRowsByColumn.get("al_entry_resolution_officer").getValue());
        Assertions.assertEquals("4", auditRowsByColumn.get("version").getValue());
    }

    @Test
    void extractAuditData_deleteAddsJoinColumnRows() {
        val audit = ApplicationListEntryResultAudit.from(createResolution());

        // Delete audit should preserve both join-column ids so the removed row can be reconstructed
        // from the audit trail.
        val auditRowsByColumn =
                audit.extractAuditData(CrudEnum.DELETE).stream()
                        .collect(
                                Collectors.toMap(
                                        AuditableData::getFieldName,
                                        Function.identity(),
                                        (left, right) -> right));

        Assertions.assertEquals("11", auditRowsByColumn.get("aler_id").getValue());
        Assertions.assertEquals("22", auditRowsByColumn.get("ale_ale_id").getValue());
        Assertions.assertEquals("33", auditRowsByColumn.get("rc_rc_id").getValue());
        Assertions.assertEquals(
                "Updated wording", auditRowsByColumn.get("al_entry_resolution_wording").getValue());
        Assertions.assertEquals(
                "Officer", auditRowsByColumn.get("al_entry_resolution_officer").getValue());
        Assertions.assertEquals("4", auditRowsByColumn.get("version").getValue());
    }

    private AppListEntryResolution createResolution() {
        val entry = new ApplicationListEntry();
        entry.setId(22L);

        val resolutionCode = new ResolutionCode();
        resolutionCode.setId(33L);

        val resolution = new AppListEntryResolution();
        resolution.setId(11L);
        resolution.setApplicationList(entry);
        resolution.setResolutionCode(resolutionCode);
        resolution.setResolutionWording("Updated wording");
        resolution.setResolutionOfficer("Officer");
        resolution.setVersion(4L);

        return resolution;
    }
}
