package uk.gov.hmcts.appregister.courtlocation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse_;

public class CourtLocationSortFieldMapperTest {

    @Test
    void lookupByApiValue_nameReturnsTitle() {
        SortableOperationEnum actual = CourtLocationSortFieldEnum.getEntityValue("name");
        assertSame(CourtLocationSortFieldEnum.TITLE, actual);
    }

    @Test
    void lookupByApiValue_codeReturnsCode() {
        SortableOperationEnum actual = CourtLocationSortFieldEnum.getEntityValue("code");
        assertSame(CourtLocationSortFieldEnum.CODE, actual);
    }

    @Test
    void lookupByApiValue_unknownReturnsNull() {
        assertNull(CourtLocationSortFieldEnum.getEntityValue("unknown"));
    }

    @Test
    void fieldsMatchExpectedContract() {
        assertEquals("name", CourtLocationSortFieldEnum.TITLE.getApiValue());
        assertEquals(
                NationalCourtHouse_.NAME, CourtLocationSortFieldEnum.TITLE.getEntityValue()[0]);

        assertEquals("code", CourtLocationSortFieldEnum.CODE.getApiValue());
        assertEquals(
                NationalCourtHouse_.COURT_LOCATION_CODE,
                CourtLocationSortFieldEnum.CODE.getEntityValue()[0]);
    }
}
