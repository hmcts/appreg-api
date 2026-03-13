package uk.gov.hmcts.appregister.courtlocation.mapper;

import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;

public class CourtLocationMapperTest {
    @Test
    public void testToSummaryDto_provideValidData_validDtoGenerated() {
        var code = "12345";
        var name = "Bath Crown Court";

        var court = new NationalCourtHouse();
        court.setCourtLocationCode(code);
        court.setName(name);

        var mapper = new CourtLocationMapperImpl();
        var dto = mapper.toSummaryDto(court);

        Assertions.assertEquals(code, dto.getLocationCode());
        Assertions.assertEquals(name, dto.getName());
    }

    @Test
    public void testToDetailDto_provideAllValidData_validDtoGenerated() {
        var code = "12345";
        var name = "Bath Crown Court";
        var startDate = LocalDate.parse("2020-01-01");
        var endDate = LocalDate.parse("2020-01-02");

        var court = new NationalCourtHouse();
        court.setCourtLocationCode(code);
        court.setName(name);
        court.setStartDate(startDate);
        court.setEndDate(endDate);

        var mapper = new CourtLocationMapperImpl();
        var dto = mapper.toDetailDto(court);

        Assertions.assertEquals(code, dto.getLocationCode());
        Assertions.assertEquals(name, dto.getName());
        Assertions.assertEquals(startDate, dto.getStartDate());
        Assertions.assertEquals(endDate, dto.getEndDate().get());
    }

    @Test
    void testToDetailDto_withNullEndDate_mapsToExplicitNull() {
        var code = "67890";
        var name = "Bristol Magistrates Court";
        var startDate = LocalDate.parse("2021-05-01");

        var court = new NationalCourtHouse();
        court.setCourtLocationCode(code);
        court.setName(name);
        court.setStartDate(startDate);
        court.setEndDate(null);

        var mapper = new CourtLocationMapperImpl();
        var dto = mapper.toDetailDto(court);

        Assertions.assertEquals(code, dto.getLocationCode());
        Assertions.assertEquals(name, dto.getName());
        Assertions.assertEquals(startDate, dto.getStartDate());

        Assertions.assertNull(dto.getEndDate().get(), "endDate should be marked present");
    }

    @Test
    void testNoEntity() {
        CodeAndName record = new CodeAndName(null, null);

        var mapper = new CourtLocationMapperImpl();
        Assertions.assertNotNull(mapper.toEntity(record));
    }
}
