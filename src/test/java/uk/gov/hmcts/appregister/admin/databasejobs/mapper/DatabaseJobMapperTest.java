package uk.gov.hmcts.appregister.admin.databasejobs.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapperImpl;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

public class DatabaseJobMapperTest {
    private final DatabaseJobsMapper mapper = new DatabaseJobsMapperImpl();

    @Test
    public void testMapYesOrNoToBoolean() {
        // Given
        var yes = YesOrNo.YES;
        var no = YesOrNo.NO;

        // When
        Boolean yesResult = mapper.map(yes);
        Boolean noResult = mapper.map(no);

        // Then
        assertNotNull(yesResult);
        assertEquals(true, yesResult);

        assertNotNull(noResult);
        assertEquals(false, noResult);
    }

    @Test
    public void testToDatabaseJobStatus() {
        // Given
        var databaseJob = new DatabaseJob();
        databaseJob.setLastRan(OffsetDateTime.now());
        databaseJob.setEnabled(YesOrNo.YES);

        // When
        var status = mapper.toDatabaseJobStatus(databaseJob);

        // Then
        assertNotNull(status);
        assertEquals(status.getLastRan(), databaseJob.getLastRan());
        assertEquals(status.getEnabled(), databaseJob.getEnabled().isYes());
    }
}
