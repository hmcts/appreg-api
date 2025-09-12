package uk.gov.hmcts.appregister.common.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;
import uk.gov.hmcts.appregister.testutils.data.DataAuditTestData;

@Slf4j
public class DataAuditRepositoryTest extends BasePostgresIntegrationTest {

    @Autowired private DataAuditRepository dataAuditRepository;

    @Autowired private UserProvider loggedInUser;

    @Test
    public void testBasicInsertionUpdate() throws Exception {
        // test save
        DataAudit dataAudit = persistance.save(new DataAuditTestData().someMinimal().build());

        // test get
        Optional<DataAudit> dataAuditToAssertAgainst =
                dataAuditRepository.findById(dataAudit.getId());

        expectAllCommonEntityFields(dataAudit, dataAuditToAssertAgainst);

        // assert that the data that has been retrieved aligns with the data that we have stored
        assertEquals(dataAudit.getSchemaName(), dataAuditToAssertAgainst.get().getSchemaName());
        assertEquals(dataAudit.getTableName(), dataAuditToAssertAgainst.get().getTableName());
        assertEquals(dataAudit.getColumnName(), dataAuditToAssertAgainst.get().getColumnName());
    }
}
