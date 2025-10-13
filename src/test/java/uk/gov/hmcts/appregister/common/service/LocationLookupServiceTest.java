package uk.gov.hmcts.appregister.common.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class LocationLookupServiceTest {

    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;

    @InjectMocks private LocationLookupService service;

    // -------- getActiveCourtOrThrow --------

    @Test
    void getActiveCourtOrThrow_validTrimmedCode_returnsSingleCourt() {
        NationalCourtHouse court = new NationalCourtHouse();
        when(courtHouseRepository.findActiveCourts("ABC123")).thenReturn(List.of(court));

        NationalCourtHouse result = service.getActiveCourtOrThrow("ABC123");

        assertSame(court, result);
        verify(courtHouseRepository).findActiveCourts("ABC123");
    }

    @Test
    void getActiveCourtOrThrow_codeWithWhitespace_trimsAndReturnsSingleCourt() {
        NationalCourtHouse court = new NationalCourtHouse();
        when(courtHouseRepository.findActiveCourts("ABC123")).thenReturn(List.of(court));

        NationalCourtHouse result = service.getActiveCourtOrThrow("  ABC123  ");

        assertSame(court, result);
        verify(courtHouseRepository).findActiveCourts("ABC123");
    }

    @Test
    void getActiveCourtOrThrow_nullCode_throwsNullPointerException() {
        NullPointerException ex =
                assertThrows(NullPointerException.class, () -> service.getActiveCourtOrThrow(null));
        assertTrue(ex.getMessage().contains("court location code must not be null"));
        verifyNoInteractions(courtHouseRepository);
    }

    @Test
    void getActiveCourtOrThrow_blankCode_throwsIllegalArgumentException() {
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class, () -> service.getActiveCourtOrThrow("   "));
        assertTrue(ex.getMessage().contains("court location code must not be blank"));
        verifyNoInteractions(courtHouseRepository);
    }

    @Test
    void getActiveCourtOrThrow_noMatch_throwsAppRegistryException() {
        when(courtHouseRepository.findActiveCourts("XYZ")).thenReturn(List.of());

        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class, () -> service.getActiveCourtOrThrow("XYZ"));
        assertTrue(ex.getMessage().contains("No court found for code 'XYZ'"));
        verify(courtHouseRepository).findActiveCourts("XYZ");
    }

    @Test
    void getActiveCourtOrThrow_multipleMatches_throwsAppRegistryException() {
        when(courtHouseRepository.findActiveCourts("DUPE"))
                .thenReturn(List.of(new NationalCourtHouse(), new NationalCourtHouse()));

        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class, () -> service.getActiveCourtOrThrow("DUPE"));
        assertTrue(ex.getMessage().contains("Multiple courts found for code 'DUPE'"));
        verify(courtHouseRepository).findActiveCourts("DUPE");
    }

    // -------- getCjaOrThrow --------

    @Test
    void getCjaOrThrow_validTrimmedCode_returnsSingleCja() {
        CriminalJusticeArea cja = new CriminalJusticeArea();
        when(cjaRepository.findByCode("52")).thenReturn(List.of(cja));

        CriminalJusticeArea result = service.getCjaOrThrow("52");

        assertSame(cja, result);
        verify(cjaRepository).findByCode("52");
    }

    @Test
    void getCjaOrThrow_codeWithWhitespace_trimsAndReturnsSingleCja() {
        CriminalJusticeArea cja = new CriminalJusticeArea();
        when(cjaRepository.findByCode("52")).thenReturn(List.of(cja));

        CriminalJusticeArea result = service.getCjaOrThrow("  52  ");

        assertSame(cja, result);
        verify(cjaRepository).findByCode("52");
    }

    @Test
    void getCjaOrThrow_nullCode_throwsNullPointerException() {
        NullPointerException ex =
                assertThrows(NullPointerException.class, () -> service.getCjaOrThrow(null));
        assertTrue(ex.getMessage().contains("CJA code must not be null"));
        verifyNoInteractions(cjaRepository);
    }

    @Test
    void getCjaOrThrow_blankCode_throwsIllegalArgumentException() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.getCjaOrThrow("   "));
        assertTrue(ex.getMessage().contains("CJA code must not be blank"));
        verifyNoInteractions(cjaRepository);
    }

    @Test
    void getCjaOrThrow_noMatch_throwsAppRegistryException() {
        when(cjaRepository.findByCode("X1")).thenReturn(List.of());

        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> service.getCjaOrThrow("X1"));
        assertTrue(ex.getMessage().contains("No Criminal Justice Areas found for code 'X1'"));
        verify(cjaRepository).findByCode("X1");
    }

    @Test
    void getCjaOrThrow_multipleMatches_throwsAppRegistryException() {
        when(cjaRepository.findByCode("Y2"))
                .thenReturn(List.of(new CriminalJusticeArea(), new CriminalJusticeArea()));

        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> service.getCjaOrThrow("Y2"));
        assertTrue(ex.getMessage().contains("Multiple Criminal Justice Areas found for code 'Y2'"));
        verify(cjaRepository).findByCode("Y2");
    }
}
