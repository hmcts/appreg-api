package uk.gov.hmcts.appregister.applicationlist.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

import java.time.LocalDate;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplicationListCreateRequestValidatorTest {

    private ApplicationListCreateRequestValidator validator;

    private enum Field { COURT, CJA, OTHER }

    // ---- HELPERS ----
    private ApplicationListCreateDto buildDto(Field... fields) {
        ApplicationListCreateDto dto = new ApplicationListCreateDto();

        for (Field f : fields) {
            switch (f) {
                case COURT -> dto.setCourtLocation(getCourtDto());
                case CJA   -> dto.setCriminalJusticeArea(getCjaDto());
                case OTHER -> dto.setOtherLocationDescription("Some other location");
            }
        }
        return dto;
    }

    private CourtLocationGetDetailDto getCourtDto() {
        var court = new CourtLocationGetDetailDto();
        court.setLocationCode("COURT-123");
        court.setName("Court Name");
        court.setStartDate(LocalDate.now());
        return court;
    }

    private CriminalJusticeAreaGetDto getCjaDto() {
        var cja = new CriminalJusticeAreaGetDto();
        cja.setCode("CJA-123");
        cja.setDescription("CJA Description");
        return cja;
    }

    @BeforeEach
    void before() { validator = new ApplicationListCreateRequestValidator(); }

    // ---- TESTS ----
    @Nested
    class ValidCombinations {

        @Test
        void valid_whenCourtLocationPresent_only() {
            var appList = buildDto(Field.COURT);
            assertDoesNotThrow(() -> validator.validate(appList));
        }

        @Test
        void valid_whenCjaAndNonBlankOtherLocation_andNoCourtLocation() {
            var appList = buildDto(Field.CJA, Field.OTHER);
            assertDoesNotThrow(() -> validator.validate(appList));
        }
    }

    @Nested
    class InvalidCombinations {

        @Test
        void invalid_whenNothingProvided() {
            var appList = buildDto();
            var ex = assertThrows(AppRegistryException.class, () -> validator.validate(appList));
            assertEquals(ApplicationListError.INVALID_LOCATION_COMBINATION.getCode(), ex.getCode());
        }

        @Test
        void invalid_whenOnlyCjaProvided() {
            var appList = buildDto(Field.CJA);
            assertThrows(AppRegistryException.class, () -> validator.validate(appList));
        }

        @Test
        void invalid_whenOnlyOtherLocationProvided() {
            var appList = buildDto(Field.OTHER);
            assertThrows(AppRegistryException.class, () -> validator.validate(appList));
        }

        @Test
        void invalid_whenAllFieldsProvided() {
            var dto = buildDto(Field.COURT, Field.CJA, Field.OTHER);
            assertThrows(AppRegistryException.class, () -> validator.validate(dto));
        }
    }
}
