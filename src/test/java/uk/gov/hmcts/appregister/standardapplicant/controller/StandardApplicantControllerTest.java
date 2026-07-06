package uk.gov.hmcts.appregister.standardapplicant.controller;

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
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPrintDto;
import uk.gov.hmcts.appregister.standardapplicant.service.StandardApplicantService;

class StandardApplicantControllerTest {
    private final StandardApplicantService service = mock(StandardApplicantService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final StandardApplicantController controller =
            new StandardApplicantController(service, pageableMapper);

    @Test
    void getStandardApplicants_delegatesAndReturnsOk() {
        var paging = mock(PagingWrapper.class);
        var body = new StandardApplicantPage();
        var from = LocalDate.of(2026, Month.JANUARY, 1);
        var to = LocalDate.of(2026, Month.DECEMBER, 31);
        when(pageableMapper.from(
                        eq(0), eq(20), eq(List.of()), any(), eq(Sort.Direction.ASC), any()))
                .thenReturn(paging);
        when(service.findAll("CODE", "Name", "Address", from, to, paging)).thenReturn(body);

        ResponseEntity<StandardApplicantPage> actual =
                controller.getStandardApplicants("CODE", "Name", "Address", from, to, 0, 20, null);

        verify(service).findAll("CODE", "Name", "Address", from, to, paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void getStandardApplicantByCode_delegatesAndReturnsVersionedOk() {
        var body = new StandardApplicantGetDetailDto().code("CODE");
        when(service.findByCode("CODE")).thenReturn(body);

        ResponseEntity<StandardApplicantGetDetailDto> actual =
                controller.getStandardApplicantByCode("CODE");

        verify(service).findByCode("CODE");
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
        assertThat(actual.getHeaders().getContentType())
                .hasToString("application/vnd.hmcts.appreg.v1+json");
    }

    @Test
    void printStandardApplicants_passesAddressLine1FilterToService() {
        var paging = mock(PagingWrapper.class);
        var body = mock(StandardApplicantPrintDto.class);
        var from = LocalDate.of(2026, Month.JANUARY, 1);
        var to = LocalDate.of(2026, Month.DECEMBER, 31);

        when(pageableMapper.from(eq(0), eq(1), eq(List.of()), any(), eq(Sort.Direction.ASC), any()))
                .thenReturn(paging);
        when(service.print("CODE", "Name", "High Street", from, to, paging)).thenReturn(body);

        ResponseEntity<StandardApplicantPrintDto> actual =
                controller.printStandardApplicants("CODE", "Name", from, to, "High Street", null);

        verify(service).print("CODE", "Name", "High Street", from, to, paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }
}
