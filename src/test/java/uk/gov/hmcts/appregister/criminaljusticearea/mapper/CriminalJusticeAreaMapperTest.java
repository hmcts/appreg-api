package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto;

public class CriminalJusticeAreaMapperTest {
    @Test
    public void testMap() {
        String code = "mycode";
        String description = "mydescription";

        // test
        CriminalJusticeArea area = new CriminalJusticeArea();
        area.setCjaCode(code);
        area.setCjaDescription(description);

        CriminalJusticeMapperImpl criminalJusticeMapper = new CriminalJusticeMapperImpl();
        CriminalJusticeAreaDto actual = criminalJusticeMapper.toDto(area);

        // assert
        Assertions.assertEquals(code, actual.getCode());
        Assertions.assertEquals(description, actual.getDescription());
    }
}
