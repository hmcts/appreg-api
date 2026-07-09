package uk.gov.hmcts.appregister.applicationentry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadCsvFormatValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadRowApplicationEntryValidator;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewResponseDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.service.JobService;

class ApplicationEntryControllerTest {
    private final ApplicationEntryService applicationEntryService =
            mock(ApplicationEntryService.class);
    private final PageableMapper pageableMapper = mock(PageableMapper.class);
    private final AsyncJobService asyncJobService = mock(AsyncJobService.class);
    private final JobService jobService = mock(JobService.class);
    private final UserProvider userProvider = mock(UserProvider.class);
    private final BulkUploadRowApplicationEntryValidator bulkUploadRowApplicationEntryValidator =
            mock(BulkUploadRowApplicationEntryValidator.class);
    private final BulkUploadApplicationEntryValidator bulkCreateApplicationEntryValidator =
            mock(BulkUploadApplicationEntryValidator.class);
    private final BulkUploadCsvFormatValidator bulkUploadCsvFormatValidator =
            mock(BulkUploadCsvFormatValidator.class);
    private final ApplicationListEntryMapper applicationListEntryMapper =
            mock(ApplicationListEntryMapper.class);
    private final Validator beanValidator = mock(Validator.class);

    private final ApplicationEntryController controller =
            new ApplicationEntryController(
                    applicationEntryService,
                    pageableMapper,
                    asyncJobService,
                    jobService,
                    userProvider,
                    bulkUploadRowApplicationEntryValidator,
                    bulkCreateApplicationEntryValidator,
                    bulkUploadCsvFormatValidator,
                    applicationListEntryMapper,
                    beanValidator);

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
    void getEntries_delegatesToServiceAndReturnsOkResponse() {
        var filter = new EntryGetFilterDto();
        var paging = mock(PagingWrapper.class);
        var page = new EntryPage();
        when(pageableMapper.from(eq(1), eq(25), eq(List.of("date")), any(), eq(Sort.Direction.ASC)))
                .thenReturn(paging);
        when(applicationEntryService.search(filter, paging)).thenReturn(page);

        ResponseEntity<EntryPage> response = controller.getEntries(filter, 1, 25, List.of("date"));

        verify(applicationEntryService).search(filter, paging);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
        assertThat(response.getHeaders().getContentType()).isEqualTo(VND_JSON_V1);
    }

    @Test
    void bulkActionPreview_delegatesToServiceAndReturnsOkResponse() {
        var request = new BulkActionPreviewRequestDto();
        var body = new BulkActionPreviewResponseDto();
        when(applicationEntryService.bulkActionPreview(request)).thenReturn(body);

        ResponseEntity<BulkActionPreviewResponseDto> response =
                controller.bulkActionPreview(request);

        verify(applicationEntryService).bulkActionPreview(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(response.getHeaders().getContentType()).isEqualTo(VND_JSON_V1);
    }

    @Test
    void createApplicationListEntry_delegatesToServiceAndReturnsCreatedResponse() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        var request = new EntryCreateDto();
        var responseBody = new EntryGetDetailDto().id(entryId);
        var response = MatchResponse.of(responseBody, List.of());
        when(applicationEntryService.createEntry(any())).thenReturn(response);

        ResponseEntity<EntryGetDetailDto> actual =
                controller.createApplicationListEntry(listId, request);

        verify(applicationEntryService).createEntry(any());
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).isSameAs(responseBody);
        assertThat(actual.getHeaders().getETag()).isEqualTo(response.getEtag());
        assertThat(actual.getHeaders().getLocation()).isNotNull();
        assertThat(actual.getHeaders().getLocation().getPath()).endsWith("/" + entryId);
    }

    @Test
    void bulkUploadApplicationListEntries_whenFileMissing_thenThrowsBadRequestError() {
        var listId = UUID.randomUUID();
        var file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        var exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> controller.bulkUploadApplicationListEntries(listId, file));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_UPLOAD_FILE_MISSING);
    }

    @Test
    void bulkUploadApplicationListEntries_delegatesAndReturnsAcceptedResponse() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        var file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("HEADER\n".getBytes()));
        doNothing().when(bulkUploadCsvFormatValidator).validate(file);
        when(userProvider.getUserId()).thenReturn("user-1");
        var jobStatus =
                JobStatusResponse.builder()
                        .uuid(jobId)
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus1.RECEIVED)
                        .userName("user-1")
                        .errorMessage(null)
                        .persistence(null)
                        .build();
        doAnswer(
                        invocation -> {
                            DataReader<?> reader = invocation.getArgument(1, DataReader.class);
                            try {
                                return new TrackJobStatusResponse(
                                        jobStatus, CompletableFuture.completedFuture(null));
                            } finally {
                                reader.close();
                            }
                        })
                .when(asyncJobService)
                .startJob(any(), anyBulkUploadRowDataReader(), anyBulkUploadRowAsyncJobLifecycle());
        var acknowledgement =
                new JobAcknowledgement()
                        .id(jobId)
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus1.RECEIVED);
        when(jobService.getJobAckById(any())).thenReturn(acknowledgement);

        ResponseEntity<JobAcknowledgement> response =
                controller.bulkUploadApplicationListEntries(listId, file);

        verify(bulkUploadCsvFormatValidator).validate(file);
        verify(asyncJobService)
                .startJob(any(), anyBulkUploadRowDataReader(), anyBulkUploadRowAsyncJobLifecycle());
        verify(jobService).getJobAckById(any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(acknowledgement);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/jobs/" + jobId);
        assertThat(response.getHeaders().getContentType()).isEqualTo(VND_JSON_V1);
    }

    @SuppressWarnings("unchecked")
    private DataReader<BulkUploadRow> anyBulkUploadRowDataReader() {
        return any(DataReader.class);
    }

    @SuppressWarnings("unchecked")
    private AsyncJobLifecycle<BulkUploadRow> anyBulkUploadRowAsyncJobLifecycle() {
        return any(AsyncJobLifecycle.class);
    }
}
