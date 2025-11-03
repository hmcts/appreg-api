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
        SortableOperationEnum actual = CourtLocationSortFieldMapper.getEntityValue("name");
        assertSame(CourtLocationSortFieldMapper.TITLE, actual);
    }

    @Test
    void lookupByApiValue_codeReturnsCode() {
        SortableOperationEnum actual = CourtLocationSortFieldMapper.getEntityValue("code");
        assertSame(CourtLocationSortFieldMapper.CODE, actual);
    }

    @Test
    void lookupByApiValue_unknownReturnsNull() {
        assertNull(CourtLocationSortFieldMapper.getEntityValue("unknown"));
    }

    @Test
    void fieldsMatchExpectedContract() {
        assertEquals("name", CourtLocationSortFieldMapper.TITLE.getApiValue());
        assertEquals(NationalCourtHouse_.NAME, CourtLocationSortFieldMapper.TITLE.getEntityValue());

        assertEquals("code", CourtLocationSortFieldMapper.CODE.getApiValue());
        assertEquals(
                NationalCourtHouse_.COURT_LOCATION_CODE,
                CourtLocationSortFieldMapper.CODE.getEntityValue());
    }
}
