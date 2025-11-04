package uk.gov.hmcts.appregister.resultcode.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.api.SortableOperationEnum;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode_;

public class ResultCodeSortFieldEnumTest {

    @Test
    void lookupByApiValue_nameReturnsTitle() {
        SortableOperationEnum actual = ResultCodeSortFieldEnum.getEntityValue("title");
        assertSame(ResultCodeSortFieldEnum.TITLE, actual);
    }

    @Test
    void lookupByApiValue_codeReturnsCode() {
        SortableOperationEnum actual = ResultCodeSortFieldEnum.getEntityValue("code");
        assertSame(ResultCodeSortFieldEnum.CODE, actual);
    }

    @Test
    void lookupByApiValue_unknownReturnsNull() {
        assertNull(ResultCodeSortFieldEnum.getEntityValue("unknown"));
    }

    @Test
    void fieldsMatchExpectedContract() {
        assertEquals("title", ResultCodeSortFieldEnum.TITLE.getApiValue());
        assertEquals(ResolutionCode_.TITLE, ResultCodeSortFieldEnum.TITLE.getEntityValue());

        assertEquals("code", ResultCodeSortFieldEnum.CODE.getApiValue());
        assertEquals(ResolutionCode_.RESULT_CODE, ResultCodeSortFieldEnum.CODE.getEntityValue());
    }
}
