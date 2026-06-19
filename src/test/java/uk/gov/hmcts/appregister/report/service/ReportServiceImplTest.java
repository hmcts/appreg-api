package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.audit.ReportJobAuditService;
import uk.gov.hmcts.appregister.report.exception.ReportError;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;
import uk.gov.hmcts.appregister.report.model.ListMaintenanceReportRow;
import uk.gov.hmcts.appregister.report.model.PrivateProsecutorsIndexReportRow;
import uk.gov.hmcts.appregister.report.model.SearchWarrantsReportRow;
import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;
import uk.gov.hmcts.appregister.report.validator.ReportLocationValidator;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock private AsyncJobService asyncJobService;
    @Mock private UserProvider userProvider;
    @Mock private JobMapper jobMapper;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private AuditOperationService auditService;
    @Mock private ReportJobAuditService reportJobAuditService;
    @Mock private ReportLocationValidator reportLocationValidator;

    private ReportServiceImpl service() {
        return new ReportServiceImpl(
                asyncJobService,
                userProvider,
                jobMapper,
                jdbcTemplate,
                auditService,
                reportJobAuditService,
                new ReportFilterNormaliser(),
                reportLocationValidator);
    }

    @Test
    void givenActivityAuditFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.ACTIVITY_AUDIT_REPORT);
        AtomicReference<ActivityAuditReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<ActivityAuditReportRow>> lifecycle =
                new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT, createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(expectedDateTo)
                        .dateTo(expectedDateFrom)
                        .activityTypes(List.of(ActivityType.BULK_APPLICATION_UPLOAD));

        try {
            ReportJobCreation result = service.createActivityAuditReport(filter);

            ActivityAuditFilterDto readerFilter =
                    (ActivityAuditFilterDto)
                            ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request ->
                                            request.getJobType() == JobType.ACTIVITY_AUDIT_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenFeesFilter_whenCreatingReport_thenStartsJobWithReportPageSize() throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.FEES_REPORT);
        AtomicReference<FeesReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<FeesReportRow>> lifecycle = new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        FeesReportFilterDto filter =
                new FeesReportFilterDto().dateFrom(expectedDateTo).dateTo(expectedDateFrom);

        try {
            ReportJobCreation result = service.createFeesReport(filter);

            FeesReportFilterDto readerFilter =
                    (FeesReportFilterDto) ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(request -> request.getJobType() == JobType.FEES_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenFeesLocationValidationFails_whenCreatingReport_thenDoesNotStartJob() {
        LegacyReportLocation location = new LegacyReportLocation().cjaCode("XX");
        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.CJA_NOT_FOUND, "No Criminal Justice Areas found for code 'XX'");
        doThrow(exception).when(reportLocationValidator).validate(location);
        runAuditPassThrough();

        ReportServiceImpl service = service();

        AppRegistryException actual =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.createFeesReport(filter));

        Assertions.assertSame(exception, actual);
        Mockito.verify(reportLocationValidator).validate(location);
        Mockito.verifyNoInteractions(asyncJobService);
    }

    @Test
    void givenSearchWarrantsFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.SEARCH_WARRANTS_REPORT);
        AtomicReference<SearchWarrantsReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<SearchWarrantsReportRow>> lifecycle =
                new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT, createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        SearchWarrantsReportFilterDto filter =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(expectedDateTo)
                        .dateTo(expectedDateFrom);

        try {
            ReportJobCreation result = service.createSearchWarrantsReport(filter);

            SearchWarrantsReportFilterDto readerFilter =
                    (SearchWarrantsReportFilterDto)
                            ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request ->
                                            request.getJobType() == JobType.SEARCH_WARRANTS_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenDurationFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.DURATION_REPORT);
        AtomicReference<DurationReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<DurationReportRow>> lifecycle = new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT, createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        DurationFilterDto filter =
                new DurationFilterDto().dateFrom(expectedDateTo).dateTo(expectedDateFrom);

        try {
            ReportJobCreation result = service.createDurationReport(filter);

            DurationFilterDto readerFilter =
                    (DurationFilterDto) ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request -> request.getJobType() == JobType.DURATION_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenWorkloadFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.WORKLOAD_REPORT);
        AtomicReference<WorkloadReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<WorkloadReportRow>> lifecycle = new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT, createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        WorkloadFilterDto filter =
                new WorkloadFilterDto().dateFrom(expectedDateTo).dateTo(expectedDateFrom);

        try {
            ReportJobCreation result = service.createWorkloadReport(filter);

            WorkloadFilterDto readerFilter =
                    (WorkloadFilterDto) ReflectionTestUtils.getField(dataReader.get(), "filterDto");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request -> request.getJobType() == JobType.WORKLOAD_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenDurationLocationValidationFails_whenCreatingReport_thenDoesNotStartJob() {
        LegacyReportLocation location = new LegacyReportLocation().courtLocationCode("BADCRT");
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.COURT_NOT_FOUND, "No court found for code 'BADCRT'");
        doThrow(exception).when(reportLocationValidator).validate(location);
        runAuditPassThrough();

        ReportServiceImpl service = service();

        AppRegistryException actual =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.createDurationReport(filter));

        Assertions.assertSame(exception, actual);
        Mockito.verify(reportLocationValidator).validate(location);
        Mockito.verifyNoInteractions(asyncJobService);
    }

    @Test
    void givenListMaintenanceFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.LIST_MAINTENANCE_REPORT);
        AtomicReference<ListMaintenanceReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<ListMaintenanceReportRow>> lifecycle =
                new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT, createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        ListMaintenanceFilterDto filter =
                new ListMaintenanceFilterDto().dateFrom(expectedDateTo).dateTo(expectedDateFrom);

        try {
            ReportJobCreation result = service.createListMaintenanceReport(filter);

            ListMaintenanceFilterDto readerFilter =
                    (ListMaintenanceFilterDto)
                            ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request ->
                                            request.getJobType()
                                                    == JobType.LIST_MAINTENANCE_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenPrivateProsecutorsIndexFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, Month.MAY, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, Month.MAY, 31);
        TrackJobStatusResponse jobResponse =
                createJobResponse(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT);
        AtomicReference<PrivateProsecutorsIndexReportDataReader> dataReader =
                new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<PrivateProsecutorsIndexReportRow>> lifecycle =
                new AtomicReference<>();
        final AtomicReference<Auditable> createdAudit = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                createdAudit);

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        PrivateProsecutorsIndexFilterDto filter =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(expectedDateTo)
                        .dateTo(expectedDateFrom)
                        .applicantSurname("Smith");

        try {
            ReportJobCreation result = service.createPrivateProsecutorsIndexReport(filter);

            PrivateProsecutorsIndexFilterDto readerFilter =
                    (PrivateProsecutorsIndexFilterDto)
                            ReflectionTestUtils.getField(dataReader.get(), "filter");
            Assertions.assertEquals(expectedDateFrom, readerFilter.getDateFrom());
            Assertions.assertEquals(expectedDateTo, readerFilter.getDateTo());
            assertAuditDateRange(result.reportParameters(), expectedDateFrom, expectedDateTo);
            Assertions.assertTrue(
                    createdAudit
                            .get()
                            .extractAuditData(CrudEnum.CREATE)
                            .contains(
                                    new AuditableData(
                                            "report_parameters", "applicantSurname", "Smith")));
            assertAuditDateRange(createdAudit.get(), expectedDateFrom, expectedDateTo);
            assertReportJobAudit(createdAudit.get(), "user-id");
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request ->
                                            request.getJobType()
                                                    == JobType.PRIVATE_PROSECUTORS_INDEX_REPORT),
                            Mockito.same(dataReader.get()),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
        } finally {
            closeLifecycle(lifecycle);
        }
    }

    @Test
    void givenListMaintenanceLocationValidationFails_whenCreatingReport_thenDoesNotStartJob() {
        LegacyReportLocation location = new LegacyReportLocation().courtLocationCode("BADCRT");
        ListMaintenanceFilterDto filter =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.COURT_NOT_FOUND, "No court found for code 'BADCRT'");
        doThrow(exception).when(reportLocationValidator).validate(location);
        runAuditPassThrough();

        ReportServiceImpl service = service();

        AppRegistryException actual =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> service.createListMaintenanceReport(filter));

        Assertions.assertSame(exception, actual);
        Mockito.verify(reportLocationValidator).validate(location);
        Mockito.verifyNoInteractions(asyncJobService);
    }

    @Test
    void givenPrivateProsecutorsLocationValidationFails_whenCreatingReport_thenDoesNotStartJob() {
        LegacyReportLocation location = new LegacyReportLocation().courtLocationCode("BADCRT");
        PrivateProsecutorsIndexFilterDto filter =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.COURT_NOT_FOUND, "No court found for code 'BADCRT'");
        doThrow(exception).when(reportLocationValidator).validate(location);
        runAuditPassThrough();

        ReportServiceImpl service = service();

        AppRegistryException actual =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> service.createPrivateProsecutorsIndexReport(filter));

        Assertions.assertSame(exception, actual);
        Mockito.verify(reportLocationValidator).validate(location);
        Mockito.verifyNoInteractions(asyncJobService);
    }

    @ParameterizedTest
    @MethodSource("validDurationLocations")
    void givenValidDurationLocationCombination_whenCreatingReport_thenStartsJob(
            LegacyReportLocation location) throws IOException {
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.DURATION_REPORT);
        AtomicReference<DurationReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<DurationReportRow>> lifecycle = new AtomicReference<>();

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            dataReader.set(invocation.getArgument(1));
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement(jobResponse));
        runAuditPassThrough();

        ReportServiceImpl service = service();
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(location);

        try {
            service.createDurationReport(filter);

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
            closeLifecycle(lifecycle);
        }
    }

    private static Stream<Arguments> validDurationLocations() {
        return Stream.of(
                Arguments.of(new LegacyReportLocation().courtLocationCode("B01IX00")),
                Arguments.of(
                        new LegacyReportLocation()
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

    private JobAcknowledgement acknowledgement(TrackJobStatusResponse jobResponse) {
        return new JobAcknowledgement()
                .id(jobResponse.getUuid())
                .type(jobResponse.getType())
                .status(jobResponse.getStatus());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runAuditAndCaptureParameters(
            ReportAuditOperation operation, AtomicReference<Auditable> auditedParameters) {
        Mockito.doAnswer(
                        invocation -> {
                            Function<BaseAuditEvent, Optional<AuditableResult>> execution =
                                    invocation.getArgument(1);
                            AuditableResult result = execution.apply(null).orElseThrow();
                            auditedParameters.set((Auditable) result.getNewEntity());
                            return result.getResultingValue();
                        })
                .when(auditService)
                .processAudit(eq(operation), any(Function.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runAuditPassThrough() {
        Mockito.doAnswer(
                        invocation -> {
                            Function<BaseAuditEvent, Optional<AuditableResult>> execution =
                                    invocation.getArgument(1);
                            return execution.apply(null).orElseThrow().getResultingValue();
                        })
                .when(auditService)
                .processAudit(any(ReportAuditOperation.class), any(Function.class));
    }

    private <T> void closeLifecycle(AtomicReference<AsyncJobLifecycle<T>> lifecycle)
            throws IOException {
        if (lifecycle.get() != null) {
            lifecycle
                    .get()
                    .lifeCycleEventPerformed(
                            new AsyncJobLifecycleEvent<>(null, List.of(), null, JobStatus1.FAILED));
        }
    }

    private void assertAuditDateRange(
            Auditable reportParameters, LocalDate expectedDateFrom, LocalDate expectedDateTo) {
        List<AuditableData> auditData = reportParameters.extractAuditData(CrudEnum.CREATE);
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData(
                                "report_parameters", "dateFrom", expectedDateFrom.toString())));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData(
                                "report_parameters", "dateTo", expectedDateTo.toString())));
    }

    private void assertReportJobAudit(Auditable auditedParameters, String expectedUserId) {
        List<AuditableData> auditData = auditedParameters.extractAuditData(CrudEnum.CREATE);
        Assertions.assertTrue(
                auditData.stream().anyMatch(data -> "jobId".equals(data.getFieldName())));
        Assertions.assertTrue(
                auditData.stream().anyMatch(data -> "reportType".equals(data.getFieldName())));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData("report_jobs", "requestingUser", expectedUserId)));
    }
}
