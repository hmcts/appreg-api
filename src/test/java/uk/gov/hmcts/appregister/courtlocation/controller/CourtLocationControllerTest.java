package uk.gov.hmcts.appregister.courtlocation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;

class CourtLocationControllerTest {
    private final CourtLocationService service = mock(CourtLocationService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final CourtLocationController controller =
            new CourtLocationController(service, pageableMapper);

    @Test
    void getCourtLocationByCodeAndDate_delegatesAndReturnsOk() {
        var date = LocalDate.of(2026, Month.JUNE, 19);
        var body = mock(CourtLocationGetDetailDto.class);
        when(service.findByCodeAndDate("CCC001", date)).thenReturn(body);

        ResponseEntity<CourtLocationGetDetailDto> actual =
                controller.getCourtLocationByCodeAndDate("CCC001", date);

        verify(service).findByCodeAndDate("CCC001", date);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void getCourtLocations_delegatesAndReturnsOk() {
        var paging = mock(PagingWrapper.class);
        var body = new CourtLocationPage();
        when(pageableMapper.from(
                        eq(0), eq(25), eq(List.of("code")), any(), eq(Sort.Direction.ASC), any()))
                .thenReturn(paging);
        when(service.getPage("name", "code", paging)).thenReturn(body);

        ResponseEntity<CourtLocationPage> actual =
                controller.getCourtLocations("name", "code", 0, 25, List.of("code"));

        verify(service).getPage("name", "code", paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }
}
