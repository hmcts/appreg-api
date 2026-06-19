package uk.gov.hmcts.appregister.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
class LocationLookupServiceTest {
    @Mock private CriminalJusticeAreaRepository cjaRepository;

    @InjectMocks private LocationLookupService service;

    @Test
    void getCjaOrThrow_validTrimmedCode_returnsSingleCja() {
        var cja = new CriminalJusticeArea();
        when(cjaRepository.findByCode("52")).thenReturn(List.of(cja));

        var result = service.getCjaOrThrow("52");

        assertSame(cja, result);
        verify(cjaRepository).findByCode("52");
    }

    @Test
    void getCjaOrThrow_noMatch_throwsAppRegistryException() {
        when(cjaRepository.findByCode("X1")).thenReturn(List.of());

        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> service.getCjaOrThrow("X1"));
        assertThat(ex.getMessage()).contains("No Criminal Justice Areas found for code 'X1'");
        verify(cjaRepository).findByCode("X1");
    }

    @Test
    void getCjaOrThrow_multipleMatches_throwsAppRegistryException() {
        when(cjaRepository.findByCode("Y2"))
                .thenReturn(List.of(new CriminalJusticeArea(), new CriminalJusticeArea()));

        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> service.getCjaOrThrow("Y2"));
        assertThat(ex.getMessage()).contains("Multiple Criminal Justice Areas found for code 'Y2'");
        verify(cjaRepository).findByCode("Y2");
    }
}
