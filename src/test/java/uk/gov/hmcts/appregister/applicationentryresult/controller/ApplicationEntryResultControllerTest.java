package uk.gov.hmcts.appregister.applicationentryresult.controller;

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
import uk.gov.hmcts.appregister.applicationentryresult.service.ApplicationEntryResultService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultsDto;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

class ApplicationEntryResultControllerTest {
    private final ApplicationEntryResultService service = mock(ApplicationEntryResultService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final ApplicationEntryResultController controller =
            new ApplicationEntryResultController(service, pageableMapper);

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
    void bulkResultApplicationListEntries_delegatesAndReturnsOk() {
        UUID listId = UUID.randomUUID();
        var request = new BulkResultDto();
        var body = List.of(new ResultGetDto());
        when(service.bulkCreate(any())).thenReturn(body);

        ResponseEntity<List<ResultGetDto>> actual =
                controller.bulkResultApplicationListEntries(listId, request);

        verify(service).bulkCreate(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void bulkResultEntries_delegatesAndReturnsOk() {
        var request = new BulkResultDto();
        var body = List.of(new ResultGetDto());
        when(service.bulkCreate(any())).thenReturn(body);

        ResponseEntity<List<ResultGetDto>> actual = controller.bulkResultEntries(request);

        verify(service).bulkCreate(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }

    @Test
    void bulkDeleteResultEntries_delegatesAndReturnsNoContent() {
        var request = new BulkDeleteResultsDto();
        doNothing().when(service).bulkDelete(any());

        ResponseEntity<Void> actual = controller.bulkDeleteResultEntries(request);

        verify(service).bulkDelete(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void createApplicationListEntryResult_delegatesAndReturnsCreated() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var request = new ResultCreateDto();
        var body = new ResultGetDto().id(resultId);
        var response = MatchResponse.of(body, List.of());
        when(service.create(any())).thenReturn(response);

        ResponseEntity<ResultGetDto> actual =
                controller.createApplicationListEntryResult(listId, entryId, request);

        verify(service).create(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).isSameAs(body);
        assertThat(actual.getHeaders().getETag()).isEqualTo(response.getEtag());
    }

    @Test
    void updateApplicationListEntryResult_delegatesAndReturnsOk() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var request = new ResultUpdateDto();
        var body = new ResultGetDto().id(resultId);
        var response = MatchResponse.of(body, List.of());
        when(service.update(any())).thenReturn(response);

        ResponseEntity<ResultGetDto> actual =
                controller.updateApplicationListEntryResult(listId, entryId, resultId, request);

        verify(service).update(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
        assertThat(actual.getHeaders().getETag()).isEqualTo(response.getEtag());
    }

    @Test
    void getApplicationListEntryResults_delegatesAndReturnsOk() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        var paging = mock(PagingWrapper.class);
        var body = new ResultPage();
        when(pageableMapper.from(
                        eq(0), eq(20), eq(List.of()), any(), eq(Sort.Direction.ASC), any()))
                .thenReturn(paging);
        when(service.search(any(), eq(paging))).thenReturn(body);

        ResponseEntity<ResultPage> actual =
                controller.getApplicationListEntryResults(listId, entryId, 0, 20);

        verify(service).search(any(), eq(paging));
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
    }
}
