package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;

public class ApplicationListCreateValidatorTest {

    private ApplicationListLocationValidator validator;

    private enum Field {
        COURT,
        CJA,
        OTHER
    }

    // ---- HELPERS ----
    private ApplicationListCreateDto buildDto(Field... fields) {
        ApplicationListCreateDto dto = new ApplicationListCreateDto();

        for (Field f : fields) {
            switch (f) {
                case COURT -> dto.setCourtLocationCode("COURT-123");
                case CJA -> dto.setCjaCode("CJA-123");
                case OTHER -> dto.setOtherLocationDescription("Some other location");
            }
        }
        return dto;
    }

    @BeforeEach
    void before() {
        validator = new ApplicationListLocationValidator();
    }

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
            AppRegistryException ex =
                    assertThrows(AppRegistryException.class, () -> validator.validate(appList));
            assertEquals(ApplicationListError.INVALID_LOCATION_COMBINATION, ex.getCode());
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
