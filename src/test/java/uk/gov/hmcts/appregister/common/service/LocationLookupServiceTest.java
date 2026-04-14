package uk.gov.hmcts.appregister.common.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import nl.altindag.log.LogCaptor;
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
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;

@ExtendWith(MockitoExtension.class)
class LocationLookupServiceTest {

    private static final LocalDate TODAY_UK = LocalDate.of(2025, 10, 7);

    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;
    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private LocationLookupService service;

    // -------- getActiveCourtOrThrow --------

    @Test
    void getActiveCourtOrThrow_validCode_returnsSingleCourt() {
        NationalCourtHouse court = new NationalCourtHouse();
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);
        when(courtHouseRepository.findActiveCourts("ABC123", TODAY_UK)).thenReturn(List.of(court));

        NationalCourtHouse result = service.getActiveCourtOrThrow("ABC123");

        assertSame(court, result);
        verify(courtHouseRepository).findActiveCourts("ABC123", TODAY_UK);
    }

    @Test
    void getActiveCourtOrThrow_noMatch_throwsAppRegistryException() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);
        when(courtHouseRepository.findActiveCourts("XYZ", TODAY_UK)).thenReturn(List.of());

        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class, () -> service.getActiveCourtOrThrow("XYZ"));
        assertTrue(ex.getMessage().contains("No court found for code 'XYZ'"));
        verify(courtHouseRepository).findActiveCourts("XYZ", TODAY_UK);
    }

    @Test
    void getActiveCourtOrThrow_multipleMatches_returnsFirstCourtAndLogsWarning() {
        NationalCourtHouse preferred = new NationalCourtHouse();
        preferred.setCourtLocationCode("DUPE");
        NationalCourtHouse alternative = new NationalCourtHouse();
        alternative.setCourtLocationCode("DUPE");
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);
        when(courtHouseRepository.findActiveCourts("DUPE", TODAY_UK))
                .thenReturn(List.of(preferred, alternative));

        LogCaptor logCaptor = LogCaptor.forClass(ReferenceDataSelectionUtil.class);

        NationalCourtHouse result = service.getActiveCourtOrThrow("DUPE");

        assertSame(preferred, result);
        assertTrue(logCaptor.getWarnLogs().getFirst().contains("Data quality warning"));
        verify(courtHouseRepository).findActiveCourts("DUPE", TODAY_UK);
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

        CriminalJusticeArea result = service.getCjaOrThrow("52");

        assertSame(cja, result);
        verify(cjaRepository).findByCode("52");
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
