package uk.gov.hmcts.appregister.report.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.exception.ReportError;

@ExtendWith(MockitoExtension.class)
class ReportLocationValidatorTest {
    private static final LocalDate TODAY_UK = LocalDate.of(2026, 5, 12);

    @Mock private CriminalJusticeAreaRepository criminalJusticeAreaRepository;
    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private ReportLocationValidator validator;

    @Test
    void givenNoLocation_whenValidating_thenDoesNotLookupReferenceData() {
        assertDoesNotThrow(() -> validator.validate(null));

        verifyNoInteractions(
                criminalJusticeAreaRepository, courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenExistingCjaAndOtherLocation_whenValidating_thenSucceeds() {
        final LegacyReportLocation location =
                new LegacyReportLocation().cjaCode("52").otherLocationDescription("Town Hall");
        when(criminalJusticeAreaRepository.findByCode("52"))
                .thenReturn(List.of(new CriminalJusticeArea()));

        assertDoesNotThrow(() -> validator.validate(location));

        verify(criminalJusticeAreaRepository).findByCode("52");
        verifyNoInteractions(courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenExistingCjaOnly_whenValidating_thenSucceeds() {
        final LegacyReportLocation location = new LegacyReportLocation().cjaCode("52");
        when(criminalJusticeAreaRepository.findByCode("52"))
                .thenReturn(List.of(new CriminalJusticeArea()));

        assertDoesNotThrow(() -> validator.validate(location));

        verify(criminalJusticeAreaRepository).findByCode("52");
        verifyNoInteractions(courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenCourtCjaAndOtherLocation_whenValidating_thenThrowsInvalidCombinationError() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .courtLocationCode("LOC123")
                        .cjaCode("52")
                        .otherLocationDescription("Town Hall");

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(location));

        assertEquals(ReportError.INVALID_LOCATION_COMBINATION, exception.getCode());
        verifyNoInteractions(
                criminalJusticeAreaRepository, courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenCourtAndCjaLocation_whenValidating_thenThrowsInvalidCombinationError() {
        LegacyReportLocation location =
                new LegacyReportLocation().courtLocationCode("LOC123").cjaCode("52");

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(location));

        assertEquals(ReportError.INVALID_LOCATION_COMBINATION, exception.getCode());
        verifyNoInteractions(
                criminalJusticeAreaRepository, courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenCourtAndOtherLocation_whenValidating_thenThrowsInvalidCombinationError() {
        LegacyReportLocation location =
                new LegacyReportLocation()
                        .courtLocationCode("LOC123")
                        .otherLocationDescription("Town Hall");

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(location));

        assertEquals(ReportError.INVALID_LOCATION_COMBINATION, exception.getCode());
        verifyNoInteractions(
                criminalJusticeAreaRepository, courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenOtherLocationOnly_whenValidating_thenSucceedsWithoutReferenceDataLookup() {
        LegacyReportLocation location =
                new LegacyReportLocation().otherLocationDescription("Town Hall");

        assertDoesNotThrow(() -> validator.validate(location));

        verifyNoInteractions(
                criminalJusticeAreaRepository, courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenCourtOnly_whenValidating_thenSucceeds() {
        LegacyReportLocation location = new LegacyReportLocation().courtLocationCode("LOC123");
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);
        when(courtHouseRepository.findActiveCourts("LOC123", TODAY_UK))
                .thenReturn(List.of(new NationalCourtHouse()));

        assertDoesNotThrow(() -> validator.validate(location));

        verify(courtHouseRepository).findActiveCourts("LOC123", TODAY_UK);
        verifyNoInteractions(criminalJusticeAreaRepository);
    }

    @Test
    void givenMissingCja_whenValidating_thenThrowsBadRequestCjaError() {
        LegacyReportLocation location =
                new LegacyReportLocation().cjaCode("XX").otherLocationDescription("Town Hall");
        when(criminalJusticeAreaRepository.findByCode("XX")).thenReturn(List.of());

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(location));

        assertEquals(ReportError.CJA_NOT_FOUND, exception.getCode());
        verify(criminalJusticeAreaRepository).findByCode("XX");
        verifyNoInteractions(courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenDuplicateCja_whenValidating_thenThrowsConflictCjaError() {
        LegacyReportLocation location =
                new LegacyReportLocation().cjaCode("ZZ").otherLocationDescription("Town Hall");
        when(criminalJusticeAreaRepository.findByCode("ZZ"))
                .thenReturn(List.of(new CriminalJusticeArea(), new CriminalJusticeArea()));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(location));

        assertEquals(ReportError.DUPLICATE_CJA_FOUND, exception.getCode());
        verify(criminalJusticeAreaRepository).findByCode("ZZ");
        verifyNoInteractions(courtHouseRepository, businessDateProvider);
    }

    @Test
    void givenMissingCourt_whenValidating_thenThrowsBadRequestCourtError() {
        LegacyReportLocation location = new LegacyReportLocation().courtLocationCode("BADCRT");
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);
        when(courtHouseRepository.findActiveCourts("BADCRT", TODAY_UK)).thenReturn(List.of());

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(location));

        assertEquals(ReportError.COURT_NOT_FOUND, exception.getCode());
        verify(courtHouseRepository).findActiveCourts("BADCRT", TODAY_UK);
        verifyNoInteractions(criminalJusticeAreaRepository);
    }
}
