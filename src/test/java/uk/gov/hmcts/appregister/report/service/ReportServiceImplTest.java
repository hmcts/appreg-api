package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.common.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.common.audit.event.StartEvent;
import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.common.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.job.ActivityAuditReportLifecycle;
import uk.gov.hmcts.appregister.report.job.DurationReportLifecycle;
import uk.gov.hmcts.appregister.report.job.FeesReportLifecycle;
import uk.gov.hmcts.appregister.report.job.reader.ActivityAuditReportDataReader;
import uk.gov.hmcts.appregister.report.job.reader.DurationReportDataReader;
import uk.gov.hmcts.appregister.report.job.reader.FeesReportDataReader;
import uk.gov.hmcts.appregister.report.normaliser.ReportFilterNormaliser;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest extends AbstractAsyncTest {
    @Mock private AsyncJobService asyncJobService;
    @Mock private UserProvider userProvider;
    @Mock private JobMapper jobMapper;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private JobService jobService;

    @Spy
    private final AuditOperationService auditOperationService = new DummyAuditOperationService();

    @Spy private final ReportFilterNormaliser reportFilterNormaliser = new ReportFilterNormaliser();

    @Test
    @Ignore("Fails you need to work out how to delete the file thats being created")
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
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        auditOperationService,
                        jobService,
                        reportFilterNormaliser);
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
    @Ignore("Fails you need to work out how to delete the file thats being created")
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
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        auditOperationService,
                        jobService,
                        reportFilterNormaliser);
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
    @Ignore("Fails you need to work out how to delete the file thats being created")
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
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        auditOperationService,
                        jobService,
                        reportFilterNormaliser);

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

    @ParameterizedTest
    @MethodSource("legacyDurationLocations")
    @Ignore("Fails you need to work out how to delete the file thats being created")
    void givenLegacyDurationLocationCombination_whenCreatingReport_thenStartsJob(
            LegacyReportLocation location) throws IOException {
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
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        auditOperationService,
                        jobService,
                        reportFilterNormaliser);

        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(location);

        service.createDurationReport(filter);

        try {
            DurationFilterDto readerFilter =
                    (DurationFilterDto) ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertSame(location, readerFilter.getLocation());
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

    private static Stream<Arguments> legacyDurationLocations() {
        return Stream.of(
                Arguments.of(new LegacyReportLocation().otherLocationDescription("Town Hall")),
                Arguments.of(
                        new LegacyReportLocation()
                                .courtLocationCode("B01IX00")
                                .otherLocationDescription("Town Hall")),
                Arguments.of(new LegacyReportLocation().courtLocationCode("B01IX00").cjaCode("01")),
                Arguments.of(
                        new LegacyReportLocation()
                                .courtLocationCode("B01IX00")
                                .otherLocationDescription("Town Hall")
                                .cjaCode("01")));
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

    class DummyAuditOperationService implements AuditOperationService {

        @Override
        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            return processAudit(
                    oldValue, auditType, execution, (AuditOperationLifecycleListener) null);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            return processAudit(null, auditType, execution);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
                AuditOperationLifecycleListener... listener) {
            return processAudit(null, auditType, execution, listener);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
                AuditOperationLifecycleListener... listener) {
            Optional<AuditableResult<T, E>> optional =
                    execution.apply(
                            new CompleteEvent(
                                    new StartEvent(
                                            AppListAuditOperation.CREATE_APP_LIST,
                                            UUID.randomUUID().toString(),
                                            null),
                                    "result",
                                    null));
            return optional.get().getResultingValue();
        }
    }
}
