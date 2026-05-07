package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

public class CriminalJusticeAreaMapperTest {
    @Test
    public void testMap() {
        String code = "mycode";
        String description = "mydescription";

        // test
        CriminalJusticeArea area = new CriminalJusticeArea();
        area.setCode(code);
        area.setDescription(description);

        CriminalJusticeMapperImpl criminalJusticeMapper = new CriminalJusticeMapperImpl();
        CriminalJusticeAreaGetDto actual = criminalJusticeMapper.toDto(area);

        // assert
        Assertions.assertEquals(code, actual.getCode());
        Assertions.assertEquals(description, actual.getDescription());
    }

    @Test
    void testNoEntity() {
        CodeAndDescription record = new CodeAndDescription(null, null);

        var mapper = new CriminalJusticeMapperImpl();
        Assertions.assertNotNull(mapper.toEntity(record));
    }
}
