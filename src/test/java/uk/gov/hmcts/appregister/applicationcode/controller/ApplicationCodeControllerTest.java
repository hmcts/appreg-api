package uk.gov.hmcts.appregister.applicationcode.controller;

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
import uk.gov.hmcts.appregister.applicationcode.service.ApplicationCodeService;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

class ApplicationCodeControllerTest {
    private final ApplicationCodeService service = mock(ApplicationCodeService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final ApplicationCodeController controller =
            new ApplicationCodeController(service, pageableMapper);

    @Test
    void getApplicationCodes_delegatesAndReturnsOk() {
        var date = LocalDate.of(2026, Month.JUNE, 19);
        var paging = mock(PagingWrapper.class);
        var body = new ApplicationCodePage();
        when(pageableMapper.from(
                        eq(1), eq(10), eq(List.of()), any(), eq(Sort.Direction.ASC), any()))
                .thenReturn(paging);
        when(service.findAll("AP", "Title", date, paging)).thenReturn(body);

        ResponseEntity<ApplicationCodePage> actual =
                controller.getApplicationCodes("AP", "Title", date, 1, 10, null);

        verify(service).findAll("AP", "Title", date, paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void getApplicationCodeByCodeAndDate_delegatesAndReturnsOk() {
        var date = LocalDate.of(2026, Month.JUNE, 19);
        var body = mock(ApplicationCodeGetDetailDto.class);
        when(service.findByCode(any())).thenReturn(body);

        ResponseEntity<ApplicationCodeGetDetailDto> actual =
                controller.getApplicationCodeByCodeAndDate("AP1", date);

        verify(service).findByCode(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }
}
