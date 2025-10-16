package uk.gov.hmcts.appregister.resultcode.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
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
        var startDateTime = LocalDateTime.parse("2020-01-01T08:30:45");
        var endDateTime = LocalDateTime.parse("2020-01-02T15:30:00");

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);
        entity.setWording(wording);
        entity.setStartDate(startDateTime);
        entity.setEndDate(endDateTime);

        ResultCodeGetDetailDto dto = mapper.toDetailDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
        Assertions.assertEquals(wording, dto.getWording());

        Assertions.assertEquals(startDateTime.toLocalDate(), dto.getStartDate());

        Assertions.assertTrue(dto.getEndDate().isPresent(), "endDate should be present");
        Assertions.assertEquals(endDateTime.toLocalDate(), dto.getEndDate().get());
    }

    @Test
    void toDetailDto_withNullEndDate_mapsToUndefined() {
        var code = "RC456";
        var title = "Dismissed";
        var wording = "Case dismissed";
        var startDateTime = LocalDateTime.parse("2021-05-01T00:00:00");

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);
        entity.setWording(wording);
        entity.setStartDate(startDateTime);
        entity.setEndDate(null);

        ResultCodeGetDetailDto dto = mapper.toDetailDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
        Assertions.assertEquals(wording, dto.getWording());
        Assertions.assertEquals(startDateTime.toLocalDate(), dto.getStartDate());

        Assertions.assertFalse(
                dto.getEndDate().isPresent(), "endDate should be undefined when source is null");
    }

    @Test
    void toDetailDto_withNullStartDate_mapsToNull() {
        var code = "RC777";
        var title = "Struck out";
        var wording = "Claim struck out";
        var endDateTime = LocalDateTime.parse("2030-12-25T23:59:59");

        var entity = new ResolutionCode();
        entity.setResultCode(code);
        entity.setTitle(title);
        entity.setWording(wording);
        entity.setStartDate(null);
        entity.setEndDate(endDateTime);

        ResultCodeGetDetailDto dto = mapper.toDetailDto(entity);

        Assertions.assertEquals(code, dto.getResultCode());
        Assertions.assertEquals(title, dto.getTitle());
        Assertions.assertEquals(wording, dto.getWording());
        Assertions.assertNull(dto.getStartDate(), "startDate should be null when source is null");

        Assertions.assertTrue(dto.getEndDate().isPresent());
        Assertions.assertEquals(endDateTime.toLocalDate(), dto.getEndDate().get());
    }

    @Test
    void helperConversions_areCorrect() {
        // These directly hit the default methods on the mapper impl.
        LocalDateTime ldt = LocalDateTime.parse("2025-10-16T14:00:00");

        LocalDate asDate = mapper.toLocalDate(ldt);
        Assertions.assertEquals(LocalDate.parse("2025-10-16"), asDate);

        JsonNullable<LocalDate> wrapped = mapper.toJsonNullableLocalDate(ldt);
        Assertions.assertTrue(wrapped.isPresent());
        Assertions.assertEquals(LocalDate.parse("2025-10-16"), wrapped.get());

        JsonNullable<LocalDate> undefined = mapper.toJsonNullableLocalDate(null);
        Assertions.assertFalse(undefined.isPresent());
    }
}
