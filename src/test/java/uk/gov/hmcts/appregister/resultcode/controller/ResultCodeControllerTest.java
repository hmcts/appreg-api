package uk.gov.hmcts.appregister.resultcode.controller;

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
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.resultcode.service.ResultCodeService;

class ResultCodeControllerTest {
    private final ResultCodeService service = mock(ResultCodeService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final ResultCodeController controller =
            new ResultCodeController(service, pageableMapper);

    @Test
    void getResultCodeByCodeAndDate_delegatesAndReturnsOk() {
        var date = LocalDate.of(2026, Month.JUNE, 19);
        var body = mock(ResultCodeGetDetailDto.class);
        when(service.findByCode("RC1", date)).thenReturn(body);

        ResponseEntity<ResultCodeGetDetailDto> actual =
                controller.getResultCodeByCodeAndDate("RC1", date);

        verify(service).findByCode("RC1", date);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void getResultCodes_delegatesAndReturnsOk() {
        var paging = mock(PagingWrapper.class);
        var body = new ResultCodePage();
        when(pageableMapper.from(
                        eq(1), eq(10), eq(List.of("code")), any(), eq(Sort.Direction.ASC), any()))
                .thenReturn(paging);
        when(service.findAll("RC", "Title", paging)).thenReturn(body);

        ResponseEntity<ResultCodePage> actual =
                controller.getResultCodes("RC", "Title", 1, 10, List.of("code"));

        verify(service).findAll("RC", "Title", paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }
}
