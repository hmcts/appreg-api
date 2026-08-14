package uk.gov.hmcts.appregister.audit;

import lombok.val;
import uk.gov.hmcts.appregister.applicationentry.audit.model.DeleteAuditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


class DeleteAuditableTest {

    @Test
    void extractAuditDataDeleteOperationSuccess() {
        val auditable = new DeleteAuditable(createApplicationListEntry());
        val auditData = auditable.extractAuditData(CrudEnum.DELETE);
        Assertions.assertThat(auditData).hasSizeGreaterThan(1);
    }

    @Test
    void extractAuditDataDeleteOperationANameAddressIsNullSuccess() {
        val applicationListEntry = createApplicationListEntry();
        applicationListEntry.setAnamedaddress(null);
        val auditable = new DeleteAuditable(applicationListEntry);
        val auditData = auditable.extractAuditData(CrudEnum.DELETE);
        Assertions.assertThat(auditData).hasSizeGreaterThan(1);

        // Want to make sure the field is included regardless of value.
        for(AuditableData auditableData : auditData) {
            if(auditableData.getFieldName().equals("a_na_id")) {
                Assertions.assertThat(auditableData.getValue()).isNull();
            }
        }
    }

    @Test
    void extractAuditDataDeleteOperationRNameAddressIsNullSuccess() {
        val applicationListEntry = createApplicationListEntry();
        applicationListEntry.setRnameaddress(null);
        val auditable = new DeleteAuditable(applicationListEntry);
        val auditData = auditable.extractAuditData(CrudEnum.DELETE);
        Assertions.assertThat(auditData).hasSizeGreaterThan(1);

        // Want to make sure the field is included regardless of value.
        for(AuditableData auditableData : auditData) {
            if(auditableData.getFieldName().equals("r_na_id")) {
                Assertions.assertThat(auditableData.getValue()).isNull();
            }
        }
    }

    @Test
    void extractAuditDataDeleteOperationFailure() {
        val auditable = new DeleteAuditable(createApplicationListEntry());
        assertThrows(
            UnsupportedOperationException.class,() -> auditable.extractAuditData(CrudEnum.CREATE));
    }

    private ApplicationListEntry createApplicationListEntry() {
      Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
      val applicationList = Instancio.of(ApplicationList.class).withSettings(settings).create();
      val applicantNameAddress = Instancio.of(NameAddress.class).withSettings(settings).create();
      val respondentNameAddress = Instancio.of(NameAddress.class).withSettings(settings).create();
      val applicationListEntry = new ApplicationListEntry();
      val resolution = Instancio.of(AppListEntryResolution.class).withSettings(settings).create();

      applicationListEntry.setUuid(UUID.randomUUID());
      applicationListEntry.setId(1L);
      applicationListEntry.setApplicationList(applicationList);
      applicationListEntry.setVersion(1L);
      applicationListEntry.setAnamedaddress(applicantNameAddress);
      applicationListEntry.setRnameaddress(respondentNameAddress);
      applicationListEntry.setResolutions(List.of(resolution));

      return applicationListEntry;
    }
}
