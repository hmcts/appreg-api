package uk.gov.hmcts.appregister.applicationlist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetByIdDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

class ApplicationListControllerTest {
    private final ApplicationListService service = mock(ApplicationListService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final ApplicationListController controller =
            new ApplicationListController(service, pageableMapper);

    @BeforeEach
    void setUpRequestContext() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createApplicationList_delegatesAndReturnsCreated() {
        UUID id = UUID.randomUUID();
        var request = new ApplicationListCreateDto();
        var body = new ApplicationListGetDetailDto().id(id);
        var response = MatchResponse.of(body, List.of());
        when(service.create(request)).thenReturn(response);

        ResponseEntity<ApplicationListGetDetailDto> actual =
                controller.createApplicationList(request);

        verify(service).create(request);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).isSameAs(body);
        assertThat(actual.getHeaders().getETag()).isEqualTo(response.getEtag());
        assertThat(actual.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void updateApplicationList_delegatesAndReturnsOk() {
        UUID id = UUID.randomUUID();
        var request = new ApplicationListUpdateDto();
        var body = new ApplicationListGetDetailDto().id(id);
        var response = MatchResponse.of(body, List.of());
        when(service.update(any())).thenReturn(response);

        ResponseEntity<ApplicationListGetDetailDto> actual =
                controller.updateApplicationList(id, request);

        verify(service).update(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
        assertThat(actual.getHeaders().getETag()).isEqualTo(response.getEtag());
    }

    @Test
    void getApplicationList_delegatesAndReturnsOk() {
        UUID id = UUID.randomUUID();
        var paging = mock(PagingWrapper.class);
        var body = new ApplicationListGetByIdDto().id(id);
        when(pageableMapper.from(
                        eq(0),
                        eq(10),
                        eq(List.of("sequenceNumber")),
                        any(),
                        eq(Sort.Direction.ASC),
                        any()))
                .thenReturn(paging);
        when(service.get(id, paging)).thenReturn(body);

        ResponseEntity<ApplicationListGetByIdDto> actual =
                controller.getApplicationList(id, 0, 10, List.of("sequenceNumber"));

        verify(service).get(id, paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void deleteApplicationList_delegatesAndReturnsNoContent() {
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        ResponseEntity<Void> actual = controller.deleteApplicationList(id);

        verify(service).delete(id);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getApplicationLists_delegatesAndReturnsOk() {
        var filter = new ApplicationListGetFilterDto();
        var paging = mock(PagingWrapper.class);
        var body = new ApplicationListPage();
        when(pageableMapper.from(
                        eq(1),
                        eq(25),
                        eq(List.of("description")),
                        any(),
                        eq(Sort.Direction.ASC),
                        any()))
                .thenReturn(paging);
        when(service.getPage(filter, paging)).thenReturn(body);

        ResponseEntity<ApplicationListPage> actual =
                controller.getApplicationLists(filter, 1, 25, List.of("description"));

        verify(service).getPage(filter, paging);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void printApplicationList_delegatesAndReturnsOk() {
        UUID id = UUID.randomUUID();
        var body = mock(ApplicationListGetPrintDto.class);
        when(service.print(id)).thenReturn(body);

        ResponseEntity<ApplicationListGetPrintDto> actual = controller.printApplicationList(id);

        verify(service).print(id);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }
}
