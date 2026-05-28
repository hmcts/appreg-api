package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
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
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
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
import uk.gov.hmcts.appregister.report.audit.ReportJobAuditService;
import uk.gov.hmcts.appregister.report.exception.ReportError;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;
import uk.gov.hmcts.appregister.report.model.ListMaintenanceReportRow;
import uk.gov.hmcts.appregister.report.model.PrivateProsecutorsIndexReportRow;
import uk.gov.hmcts.appregister.report.model.SearchWarrantsReportRow;
import uk.gov.hmcts.appregister.report.validator.ReportLocationValidator;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock private AsyncJobService asyncJobService;
    @Mock private UserProvider userProvider;
    @Mock private JobMapper jobMapper;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private ReportJobAuditService reportJobAuditService;
    @Mock private ReportLocationValidator reportLocationValidator;

    @Test
    void givenActivityAuditFilter_whenCreatingReport_thenStartsJobWithReportPageSize()
            throws IOException {
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.ACTIVITY_AUDIT_REPORT);
        AtomicReference<ActivityAuditReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<ActivityAuditReportRow>> lifecycle =
                new AtomicReference<>();

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
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
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
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.FEES_REPORT);
        AtomicReference<FeesReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<FeesReportRow>> lifecycle = new AtomicReference<>();

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
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
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
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.CJA_NOT_FOUND, "No Criminal Justice Areas found for code 'XX'");
        doThrow(exception).when(reportLocationValidator).validate(location);

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);

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
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.SEARCH_WARRANTS_REPORT);
        AtomicReference<SearchWarrantsReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<SearchWarrantsReportRow>> lifecycle =
                new AtomicReference<>();

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
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
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
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
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
        when(jobMapper.toDto(jobResponse)).thenReturn(new JobAcknowledgement());

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
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
    void givenDurationLocationValidationFails_whenCreatingReport_thenDoesNotStartJob() {
        LegacyReportLocation location = new LegacyReportLocation().courtLocationCode("BADCRT");
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.COURT_NOT_FOUND, "No court found for code 'BADCRT'");
        doThrow(exception).when(reportLocationValidator).validate(location);

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);

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
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse = createJobResponse(JobType.LIST_MAINTENANCE_REPORT);
        AtomicReference<ListMaintenanceReportDataReader> dataReader = new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<ListMaintenanceReportRow>> lifecycle =
                new AtomicReference<>();

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
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
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
        final LocalDate expectedDateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate expectedDateTo = LocalDate.of(2018, 5, 31);
        TrackJobStatusResponse jobResponse =
                createJobResponse(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT);
        AtomicReference<PrivateProsecutorsIndexReportDataReader> dataReader =
                new AtomicReference<>();
        AtomicReference<AsyncJobLifecycle<PrivateProsecutorsIndexReportRow>> lifecycle =
                new AtomicReference<>();

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
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
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
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.COURT_NOT_FOUND, "No court found for code 'BADCRT'");
        doThrow(exception).when(reportLocationValidator).validate(location);

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);

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
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(location);
        AppRegistryException exception =
                new AppRegistryException(
                        ReportError.COURT_NOT_FOUND, "No court found for code 'BADCRT'");
        doThrow(exception).when(reportLocationValidator).validate(location);

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);

        AppRegistryException actual =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> service.createPrivateProsecutorsIndexReport(filter));

        Assertions.assertSame(exception, actual);
        Mockito.verify(reportLocationValidator).validate(location);
        Mockito.verifyNoInteractions(asyncJobService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("reportTypesUsingSharedLocationValidation")
    void givenOtherLocationOnly_whenCreatingReportUsingSharedLocationValidation_thenStartsJob(
            SharedLocationReportCase<?> reportCase) throws IOException {
        TrackJobStatusResponse jobResponse = createJobResponse(reportCase.jobType());
        JobAcknowledgement acknowledgement = new JobAcknowledgement().type(reportCase.jobType());
        AtomicReference<AsyncJobLifecycle<?>> lifecycle = new AtomicReference<>();
        CriminalJusticeAreaRepository criminalJusticeAreaRepository =
                Mockito.mock(CriminalJusticeAreaRepository.class);
        NationalCourtHouseRepository courtHouseRepository =
                Mockito.mock(NationalCourtHouseRepository.class);
        BusinessDateProvider businessDateProvider = Mockito.mock(BusinessDateProvider.class);
        ReportLocationValidator sharedValidator =
                Mockito.spy(
                        new ReportLocationValidator(
                                criminalJusticeAreaRepository,
                                courtHouseRepository,
                                businessDateProvider));

        when(userProvider.getUserId()).thenReturn("user-id");
        when(asyncJobService.startJob(any(), any(), any(), any(Integer.class)))
                .thenAnswer(
                        invocation -> {
                            lifecycle.set(invocation.getArgument(2));
                            return jobResponse;
                        });
        when(jobMapper.toDto(jobResponse)).thenReturn(acknowledgement);

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        sharedValidator);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        LegacyReportLocation location =
                new LegacyReportLocation().otherLocationDescription("Town Hall");

        try {
            ReportJobCreation result = reportCase.create(service, location);

            Assertions.assertSame(acknowledgement, result.acknowledgement());
            Mockito.verify(sharedValidator).validate(location);
            Mockito.verify(asyncJobService)
                    .startJob(
                            Mockito.argThat(
                                    request -> request.getJobType() == reportCase.jobType()),
                            Mockito.any(),
                            Mockito.same(lifecycle.get()),
                            Mockito.eq(500));
            Mockito.verifyNoInteractions(
                    criminalJusticeAreaRepository, courtHouseRepository, businessDateProvider);
        } finally {
            closeLifecycle(lifecycle);
        }
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
        when(jobMapper.toDto(jobResponse)).thenReturn(new JobAcknowledgement());

        ReportServiceImpl service =
                new ReportServiceImpl(
                        asyncJobService,
                        userProvider,
                        jobMapper,
                        jdbcTemplate,
                        reportJobAuditService,
                        new ReportFilterNormaliser(),
                        reportLocationValidator);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        ReflectionTestUtils.setField(service, "reportPageSize", 500);
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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

    private static Stream<Arguments> reportTypesUsingSharedLocationValidation() {
        final LocalDate dateFrom = LocalDate.of(2018, 5, 1);
        final LocalDate dateTo = LocalDate.of(2018, 5, 31);

        return Stream.of(
                Arguments.of(
                        new SharedLocationReportCase<FeesReportFilterDto>(
                                "fees report",
                                JobType.FEES_REPORT,
                                location ->
                                        new FeesReportFilterDto()
                                                .dateFrom(dateFrom)
                                                .dateTo(dateTo)
                                                .location(location),
                                ReportServiceImpl::createFeesReport)),
                Arguments.of(
                        new SharedLocationReportCase<SearchWarrantsReportFilterDto>(
                                "search warrants report",
                                JobType.SEARCH_WARRANTS_REPORT,
                                location ->
                                        new SearchWarrantsReportFilterDto()
                                                .dateFrom(dateFrom)
                                                .dateTo(dateTo)
                                                .location(location),
                                ReportServiceImpl::createSearchWarrantsReport)),
                Arguments.of(
                        new SharedLocationReportCase<DurationFilterDto>(
                                "duration report",
                                JobType.DURATION_REPORT,
                                location ->
                                        new DurationFilterDto()
                                                .dateFrom(dateFrom)
                                                .dateTo(dateTo)
                                                .location(location),
                                ReportServiceImpl::createDurationReport)),
                Arguments.of(
                        new SharedLocationReportCase<WorkloadFilterDto>(
                                "workload report",
                                JobType.WORKLOAD_REPORT,
                                location ->
                                        new WorkloadFilterDto()
                                                .dateFrom(dateFrom)
                                                .dateTo(dateTo)
                                                .location(location),
                                ReportServiceImpl::createWorkloadReport)),
                Arguments.of(
                        new SharedLocationReportCase<ListMaintenanceFilterDto>(
                                "list maintenance report",
                                JobType.LIST_MAINTENANCE_REPORT,
                                location ->
                                        new ListMaintenanceFilterDto()
                                                .dateFrom(dateFrom)
                                                .dateTo(dateTo)
                                                .location(location),
                                ReportServiceImpl::createListMaintenanceReport)),
                Arguments.of(
                        new SharedLocationReportCase<PrivateProsecutorsIndexFilterDto>(
                                "private prosecutors index report",
                                JobType.PRIVATE_PROSECUTORS_INDEX_REPORT,
                                location ->
                                        new PrivateProsecutorsIndexFilterDto()
                                                .dateFrom(dateFrom)
                                                .dateTo(dateTo)
                                                .applicantSurname("Smith")
                                                .location(location),
                                ReportServiceImpl::createPrivateProsecutorsIndexReport)));
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void closeLifecycle(AtomicReference<? extends AsyncJobLifecycle<?>> lifecycle)
            throws IOException {
        AsyncJobLifecycle currentLifecycle = lifecycle.get();
        if (currentLifecycle != null) {
            currentLifecycle.lifeCycleEventPerformed(
                    new AsyncJobLifecycleEvent(null, List.of(), null, JobStatus1.FAILED));
        }
    }

    private interface ReportCreator<T> {
        ReportJobCreation create(ReportServiceImpl service, T filter);
    }

    private record SharedLocationReportCase<T>(
            String name,
            JobType jobType,
            Function<LegacyReportLocation, T> filterFactory,
            ReportCreator<T> creator) {
        private ReportJobCreation create(ReportServiceImpl service, LegacyReportLocation location) {
            return creator.create(service, filterFactory.apply(location));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void assertAuditDateRange(
            Auditable reportParameters, LocalDate expectedDateFrom, LocalDate expectedDateTo) {
        List<AuditableData> auditData = reportParameters.extractAuditData(CrudEnum.READ);
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData(
                                "report_parameters", "dateFrom", expectedDateFrom.toString())));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData(
                                "report_parameters", "dateTo", expectedDateTo.toString())));
    }
}
