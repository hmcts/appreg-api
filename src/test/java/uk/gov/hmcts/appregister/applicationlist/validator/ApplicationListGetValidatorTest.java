package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;

@ExtendWith(MockitoExtension.class)
public class ApplicationListGetValidatorTest {

    private static final LocalDate TODAY_UK = LocalDate.of(2025, 10, 7);

    @Mock private ApplicationListRepository repository;
    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;
    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private ApplicationListGetValidator validator;

    @BeforeEach
    void setUp() {
        lenient().when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);
    }

    private enum Field {
        COURT,
        CJA,
        OTHER
    }

    // ---- HELPERS ----
    private ApplicationListGetFilterDto buildDto(Field... fields) {
        ApplicationListGetFilterDto dto = new ApplicationListGetFilterDto();

        for (Field f : fields) {
            switch (f) {
                case COURT -> dto.setCourtLocationCode("COURT-123");
                case CJA -> dto.setCjaCode("CJA-123");
                case OTHER -> dto.setOtherLocationDescription("Some other location");
            }
        }
        return dto;
    }

    @Test
    void create_noCourtReturnedFromRepository_throwsAppRegException() {
        // given
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);
        when(dto.getCourtLocationCode()).thenReturn("CODE1");

        when(courtHouseRepository.findActiveCourts("CODE1", TODAY_UK)).thenReturn(List.of());

        // expect
        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("No court found");
    }

    @Test
    void create_multipleCourtsReturnedFromRepository_prefersFirstCourt() {
        // given
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);
        when(dto.getCourtLocationCode()).thenReturn("DUPE");

        NationalCourtHouse c1 = new NationalCourtHouse();
        NationalCourtHouse c2 = new NationalCourtHouse();
        when(courtHouseRepository.findActiveCourts("DUPE", TODAY_UK)).thenReturn(List.of(c1, c2));

        ListLocationValidationSuccess success = validator.validate(dto, (input, result) -> result);

        Assertions.assertSame(c1, success.getNationalCourtHouse());
    }

    @Test
    void create_noCjaReturnedFromRepository_throwsAppRegException() {
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);
        when(dto.getCourtLocationCode()).thenReturn(null);
        when(dto.getCjaCode()).thenReturn("X1");
        when(dto.getOtherLocationDescription()).thenReturn("Y2");

        when(cjaRepository.findByCode("X1")).thenReturn(List.of());

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(dto));
        Assertions.assertEquals(ApplicationListError.CJA_NOT_FOUND, exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void create_multipleCjaReturnedFromRepository_throwsAppRegException() {
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);
        when(dto.getCourtLocationCode()).thenReturn("");
        when(dto.getCjaCode()).thenReturn("Y2");
        when(dto.getOtherLocationDescription()).thenReturn("Y2");

        CriminalJusticeArea a = new CriminalJusticeArea();
        CriminalJusticeArea b = new CriminalJusticeArea();
        when(cjaRepository.findByCode("Y2")).thenReturn(List.of(a, b));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(dto));
        Assertions.assertEquals(ApplicationListError.DUPLICATE_CJA_FOUND, exception.getCode());

        verify(repository, never()).save(any());
    }

    @Test
    void create_noCjaReturnedFromRepositoryWhereDoNotFailOnMissingFalse_throwsAppRegException() {
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);
        when(dto.getCjaCode()).thenReturn("X1");
        when(cjaRepository.findByCode("X1")).thenReturn(List.of());

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> validator.validateCja(dto, (testDto, success) -> null, false));
        Assertions.assertEquals(ApplicationListError.CJA_NOT_FOUND, exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void create_noCjaPassedWhereDoNotFailOnMissingTrue_throwsAppRegException() {
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);

        validator.validateCja(dto, (testDto, success) -> null, true);
    }

    @Test
    void
            create_multipleCjaReturnedFromRepositoryWhereDoNotFailOnMissingFalse_throwsAppRegException() {
        ApplicationListGetFilterDto dto = mock(ApplicationListGetFilterDto.class);
        when(dto.getCjaCode()).thenReturn("Y2");
        CriminalJusticeArea a = new CriminalJusticeArea();
        CriminalJusticeArea b = new CriminalJusticeArea();
        when(cjaRepository.findByCode("Y2")).thenReturn(List.of(a, b));

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> validator.validateCja(dto, (testDto, success) -> null, false));
        Assertions.assertEquals(ApplicationListError.DUPLICATE_CJA_FOUND, exception.getCode());

        verify(repository, never()).save(any());
    }

    // ---- TESTS ----
    @Nested
    class ValidCombinations {

        @Test
        void valid_whenCourtLocationPresent_only() {
            var appList = buildDto(Field.COURT);
            when(courtHouseRepository.findActiveCourts(appList.getCourtLocationCode(), TODAY_UK))
                    .thenReturn(List.of(new NationalCourtHouse()));
            assertDoesNotThrow(() -> validator.validate(appList));
        }

        @Test
        void valid_whenCourtLocationPresentWithCallback_only() {
            var appList = buildDto(Field.COURT);
            when(courtHouseRepository.findActiveCourts(appList.getCourtLocationCode(), TODAY_UK))
                    .thenReturn(List.of(new NationalCourtHouse()));

            BiFunction<ApplicationListGetFilterDto, ListLocationValidationSuccess, ?> callback =
                    (dto, success) -> "result";
            Assertions.assertEquals("result", validator.validate(appList, callback));
        }

        @Test
        void valid_whenCjaAndNonBlankOtherLocation_andNoCourtLocation() {
            var appList = buildDto(Field.CJA, Field.OTHER);
            when(cjaRepository.findByCode(appList.getCjaCode()))
                    .thenReturn(List.of(new CriminalJusticeArea()));

            assertDoesNotThrow(() -> validator.validate(appList));
        }

        @Test
        void valid_whenCjaAndNonBlankOtherLocationWithCallback_andNoCourtLocation() {
            var appList = buildDto(Field.CJA, Field.OTHER);
            when(cjaRepository.findByCode(appList.getCjaCode()))
                    .thenReturn(List.of(new CriminalJusticeArea()));

            BiFunction<ApplicationListGetFilterDto, ListLocationValidationSuccess, ?> callback =
                    (dto, success) -> "result";
            Assertions.assertEquals("result", validator.validate(appList, callback));
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
