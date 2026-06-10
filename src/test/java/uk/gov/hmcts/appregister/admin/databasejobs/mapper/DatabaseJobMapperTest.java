package uk.gov.hmcts.appregister.admin.databasejobs.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapperImpl;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

class DatabaseJobMapperTest {
    private static final OffsetDateTime LAST_RAN = OffsetDateTime.parse("2025-01-02T03:04:05Z");

    private final DatabaseJobsMapper mapper = new DatabaseJobsMapperImpl();

    @Test
    void testMapYesOrNoToBoolean() {
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
    void testToDatabaseJobStatus() {
        // Given
        var databaseJob = new DatabaseJob();
        databaseJob.setLastRan(LAST_RAN);
        databaseJob.setEnabled(YesOrNo.YES);

        // When
        var status = mapper.toDatabaseJobStatus(databaseJob);

        // Then
        assertNotNull(status);
        assertEquals(status.getLastRan(), databaseJob.getLastRan());
        assertEquals(status.getEnabled(), databaseJob.getEnabled().isYes());
    }
}
