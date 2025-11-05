package uk.gov.hmcts.appregister.resultcode.mapper;

import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;

public class ResultCodeMapperTest {
    private final ResultCodeMapper mapper = new ResultCodeMapperImpl();

    @Test
    void toSummaryDto_provideValidData_validDtoGenerated() {
        var code = "RC123";
        var title = "Convicted";

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);

        ResultCodeGetSummaryDto dto = mapper.toSummaryDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
    }

    @Test
    void toDetailDto_provideAllValidData_validDtoGenerated() {
        var code = "RC999";
        var title = "Acquitted";
        var wording = "Defendant acquitted on all counts";
        var startDate = LocalDate.parse("2020-01-01");
        var endDate = LocalDate.parse("2020-01-02");

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);
        entity.setWording(wording);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);

        ResultCodeGetDetailDto dto = mapper.toDetailDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
        Assertions.assertEquals(wording, dto.getWording());
        Assertions.assertEquals(startDate, dto.getStartDate());
        Assertions.assertEquals(endDate, dto.getEndDate().get());
    }

    @Test
    void toDetailDto_withNullEndDate_mapsToUndefined() {
        var code = "RC456";
        var title = "Dismissed";
        var wording = "Case dismissed";
        var startDate = LocalDate.parse("2021-05-01");

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);
        entity.setWording(wording);
        entity.setStartDate(startDate);
        entity.setEndDate(null);

        ResultCodeGetDetailDto dto = mapper.toDetailDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
        Assertions.assertEquals(wording, dto.getWording());
        Assertions.assertEquals(startDate, dto.getStartDate());

        Assertions.assertFalse(
                dto.getEndDate().isPresent(), "endDate should be undefined when source is null");
    }

    @Test
    void toDetailDto_withNullStartDate_mapsToNull() {
        var code = "RC777";
        var title = "Struck out";
        var wording = "Claim struck out";
        var endDate = LocalDate.parse("2030-12-25");

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);
        entity.setWording(wording);
        entity.setStartDate(null);
        entity.setEndDate(endDate);

        ResultCodeGetDetailDto dto = mapper.toDetailDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
        Assertions.assertEquals(wording, dto.getWording());
        Assertions.assertNull(dto.getStartDate(), "startDate should be null when source is null");
        Assertions.assertEquals(endDate, dto.getEndDate().get());
    }
}
