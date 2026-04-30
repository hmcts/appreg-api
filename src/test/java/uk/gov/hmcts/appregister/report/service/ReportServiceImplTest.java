package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.Location;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.report.exception.ReportError;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock private AsyncJobService asyncJobService;
    @Mock private UserProvider userProvider;
    @Mock private JobMapper jobMapper;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void givenActivityAuditFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.ACTIVITY_AUDIT_REPORT);
        AtomicReference<ActivityAuditReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<ActivityAuditReportLifecycle> lifecycle = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(new JobAcknowledgement());

        ReportServiceImpl service =
                new ReportServiceImpl(asyncJobService, userProvider, jobMapper, jdbcTemplate);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(expectedDateFrom)
                        .dateTo(expectedDateTo)
                        .activityTypes(List.of(ActivityType.BULK_APPLICATION_UPLOAD));

        service.createActivityAuditReport(filter);

        try {
            ActivityAuditFilterDto readerFilter =
                    (ActivityAuditFilterDto)
                            ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request ->
                                            request.getJobType() == JobType.ACTIVITY_AUDIT_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            lifecycle
                    .get()
                    .failed(new AsyncJobLifecycleEvent<>(null, List.of(), null, JobStatus1.FAILED));
        }
    }

    @Test
    void givenFeesFilter_whenCreatingReport_thenStartsJobWithReportPageSize() throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.FEES_REPORT);
        AtomicReference<FeesReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<FeesReportLifecycle> lifecycle = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(new JobAcknowledgement());

        ReportServiceImpl service =
                new ReportServiceImpl(asyncJobService, userProvider, jobMapper, jdbcTemplate);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        FeesReportFilterDto filter =
                new FeesReportFilterDto().dateFrom(expectedDateFrom).dateTo(expectedDateTo);

        service.createFeesReport(filter);

        try {
            FeesReportFilterDto readerFilter =
                    (FeesReportFilterDto) ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(request -> request.getJobType() == JobType.FEES_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            lifecycle
                    .get()
                    .failed(new AsyncJobLifecycleEvent<>(null, List.of(), null, JobStatus1.FAILED));
        }
    }

    @Test
    void givenDurationFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.DURATION_REPORT);
        AtomicReference<DurationReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<DurationReportLifecycle> lifecycle = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(new JobAcknowledgement());

        ReportServiceImpl service =
                new ReportServiceImpl(asyncJobService, userProvider, jobMapper, jdbcTemplate);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        DurationFilterDto filter =
                new DurationFilterDto().dateFrom(expectedDateFrom).dateTo(expectedDateTo);

        service.createDurationReport(filter);

        try {
            DurationFilterDto readerFilter =
                    (DurationFilterDto) ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request -> request.getJobType() == JobType.DURATION_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            lifecycle
                    .get()
                    .failed(new AsyncJobLifecycleEvent<>(null, List.of(), null, JobStatus1.FAILED));
        }
    }

    @Test
    void givenDurationCourtAndCjaLocation_whenCreatingReport_thenRejectsLocationCombination() {
        ReportServiceImpl service =
                new ReportServiceImpl(asyncJobService, userProvider, jobMapper, jdbcTemplate);
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(new Location().courtLocationCode("B01IX00").cjaCode("01"));

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.createDurationReport(filter));

        Assertions.assertEquals(ReportError.INVALID_LOCATION_COMBINATION, exception.getCode());
        Mockito.verifyNoInteractions(userProvider, asyncJobService, jobMapper);
    }

    @Test
    void givenDurationCourtAndOtherLocation_whenCreatingReport_thenRejectsLocationCombination() {
        ReportServiceImpl service =
                new ReportServiceImpl(asyncJobService, userProvider, jobMapper, jdbcTemplate);
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(
                                new Location()
                                        .courtLocationCode("B01IX00")
                                        .otherLocationDescription("Town Hall"));

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.createDurationReport(filter));

        Assertions.assertEquals(ReportError.INVALID_LOCATION_COMBINATION, exception.getCode());
        Mockito.verifyNoInteractions(userProvider, asyncJobService, jobMapper);
    }

    private TrackJobStatusResponse createJobResponse(JobType jobType) {
        JobStatusResponse response =
                JobStatusResponse.builder()
                        .uuid(UUID.randomUUID())
                        .type(jobType)
                        .status(JobStatus1.RECEIVED)
                        .userName("user-id")
                        .persistence(Mockito.mock(AsyncJobPersistenceService.class))
                        .build();
        return new TrackJobStatusResponse(response, CompletableFuture.completedFuture(null));
    }
}
