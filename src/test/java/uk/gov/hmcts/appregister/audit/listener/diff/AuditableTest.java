package uk.gov.hmcts.appregister.audit.listener.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

class AuditableTest {

    @Test
    void extractAuditData_delegatesToReflectiveAuditorForMatchingCrudType() {
        var auditable = new TestAuditable(12L, "value");

        List<AuditableData> auditData = auditable.extractAuditData(CrudEnum.UPDATE);

        assertEquals(1, auditData.size());
        assertEquals("test_table", auditData.getFirst().getTableName());
        assertEquals("test_column", auditData.getFirst().getFieldName());
        assertEquals("value", auditData.getFirst().getValue());
    }

    @Test
    void extractAuditData_returnsEmptyWhenCrudTypeIsNotEnabled() {
        var auditable = new TestAuditable(12L, "value");

        assertTrue(auditable.extractAuditData(CrudEnum.CREATE).isEmpty());
    }

    @Table(name = "test_table")
    @AuditEnabled(types = {CrudEnum.UPDATE})
    private static final class TestAuditable implements Auditable {
        private final Long id;

        @Audit(action = {CrudEnum.UPDATE})
        @Column(name = "test_column")
        private final String value;

        private TestAuditable(Long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }
}
