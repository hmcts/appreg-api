package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

class ApplicationListEntryReadAuditTest {

    @Test
    void extractAuditData_appendsResultedRowToMappedEntryAuditData() {
        // Build the same kind of database-backed surrogate the service passes into the read audit
        // wrapper, then add a resulted filter value that should be written as a joined audit row.
        val applicationList = new ApplicationList();
        applicationList.setId(123L);
        applicationList.setStatus(Status.OPEN);

        val applicationCode = new ApplicationCode();
        applicationCode.setTitle("Read audit application title");
        applicationCode.setFeeDue(YesOrNo.YES);

        val applicant = new NameAddress();
        applicant.setCode(NameAddressCodeType.APPLICANT);
        applicant.setName("Applicant Audit Org");

        val respondent = new NameAddress();
        respondent.setCode(NameAddressCodeType.RESPONDENT);
        respondent.setName("Respondent Audit Org");
        respondent.setPostcode("ZZ1 1ZZ");

        val applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setId(0L);
        applicationListEntry.setApplicationList(applicationList);
        applicationListEntry.setApplicationCode(applicationCode);
        applicationListEntry.setAnamedaddress(applicant);
        applicationListEntry.setRnameaddress(respondent);
        applicationListEntry.setAccountNumber("ACC-123");
        applicationListEntry.setSequenceNumber((short) 7);

        val audit = new ApplicationListEntryReadAudit(applicationListEntry, "RC1");

        // Execute the same extraction path used by the data-audit logger for successful GETs.
        val auditData = audit.extractAuditData(CrudEnum.READ);

        // Assert the ordinary mapped entity rows are still present.
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "account_number",
                        "ACC-123"));
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_CODES,
                        "application_code_title",
                        "Read audit application title"));

        // The wrapper's only special behaviour is to append the joined result-code filter row.
        Assertions.assertTrue(
                containsAuditRow(auditData, TableNames.RESOLUTION_CODES, "resolution_code", "RC1"));
    }

    private boolean containsAuditRow(
            List<AuditableData> auditData, String tableName, String fieldName, String value) {
        return auditData.stream()
                .anyMatch(
                        row ->
                                tableName.equals(row.getTableName())
                                        && fieldName.equals(row.getFieldName())
                                        && value.equals(row.getValue()));
    }
}
