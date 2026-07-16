package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ActivityAuditReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.DurationReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.FeesReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ListMaintenanceReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.PrivateProsecutorsIndexReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.audit.ReportJobAudit;
import uk.gov.hmcts.appregister.report.audit.ReportJobAuditService;
import uk.gov.hmcts.appregister.report.audit.SearchWarrantsReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.WorkloadReportParameterAudit;
import uk.gov.hmcts.appregister.report.validator.ReportLocationValidator;

@Service
public class ReportServiceImpl implements ReportService {
    public static final String REPORT_DOWNLOAD_FILENAME = "report.csv";

    private final AsyncJobService asyncJobService;
    private final JobService jobService;
    private final UserProvider userProvider;
    private final JobMapper jobMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditOperationService auditService;
    private final ReportJobAuditService reportJobAuditService;
    private final ReportFilterNormaliser reportFilterNormaliser;
    private final ReportLocationValidator reportLocationValidator;
    private final String schema;
    private final int reportPageSize;

    public ReportServiceImpl(
            AsyncJobService asyncJobService,
            JobService jobService,
            UserProvider userProvider,
            JobMapper jobMapper,
            NamedParameterJdbcTemplate jdbcTemplate,
            AuditOperationService auditService,
            ReportJobAuditService reportJobAuditService,
            ReportFilterNormaliser reportFilterNormaliser,
            ReportLocationValidator reportLocationValidator,
            @Value("${spring.jpa.properties.hibernate.default_schema}") String schema,
            @Value("${appreg.report.page-size}") int reportPageSize) {
        this.asyncJobService = asyncJobService;
        this.jobService = jobService;
        this.userProvider = userProvider;
        this.jobMapper = jobMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.reportJobAuditService = reportJobAuditService;
        this.reportFilterNormaliser = reportFilterNormaliser;
        this.reportLocationValidator = reportLocationValidator;
        this.schema = schema;
        this.reportPageSize = reportPageSize;
    }

    @Override
    public ReportJobCreation createActivityAuditReport(ActivityAuditFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                () -> {
                    ActivityAuditFilterDto normalisedFilter =
                            reportFilterNormaliser.normalise(filter);
                    ActivityAuditReportLifecycle lifecycle;
                    try {
                        lifecycle = new ActivityAuditReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create activity audit report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.ACTIVITY_AUDIT_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new ActivityAuditReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement,
                            ActivityAuditReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportJobCreation createFeesReport(FeesReportFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT,
                () -> {
                    FeesReportFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
                    reportLocationValidator.validate(normalisedFilter.getLocation());
                    FeesReportLifecycle lifecycle;
                    try {
                        lifecycle = new FeesReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create fees report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.FEES_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new FeesReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement, FeesReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportJobCreation createSearchWarrantsReport(SearchWarrantsReportFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT,
                () -> {
                    SearchWarrantsReportFilterDto normalisedFilter =
                            reportFilterNormaliser.normalise(filter);
                    reportLocationValidator.validate(normalisedFilter.getLocation());
                    SearchWarrantsReportLifecycle lifecycle;

                    try {
                        lifecycle = new SearchWarrantsReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create search warrant report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.SEARCH_WARRANTS_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new SearchWarrantsReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement,
                            SearchWarrantsReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportJobCreation createDurationReport(DurationFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT,
                () -> {
                    DurationFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
                    reportLocationValidator.validate(normalisedFilter.getLocation());
                    DurationReportLifecycle lifecycle;
                    try {
                        lifecycle = new DurationReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create duration report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.DURATION_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new DurationReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement, DurationReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportJobCreation createWorkloadReport(WorkloadFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT,
                () -> {
                    WorkloadFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
                    reportLocationValidator.validate(normalisedFilter.getLocation());
                    WorkloadReportLifecycle lifecycle;
                    try {
                        lifecycle = new WorkloadReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create workload report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.WORKLOAD_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new WorkloadReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement, WorkloadReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportJobCreation createListMaintenanceReport(ListMaintenanceFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT,
                () -> {
                    ListMaintenanceFilterDto normalisedFilter =
                            reportFilterNormaliser.normalise(filter);
                    reportLocationValidator.validate(normalisedFilter.getLocation());
                    ListMaintenanceReportLifecycle lifecycle;
                    try {
                        lifecycle = new ListMaintenanceReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create list maintenance report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.LIST_MAINTENANCE_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new ListMaintenanceReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement,
                            ListMaintenanceReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportJobCreation createPrivateProsecutorsIndexReport(
            PrivateProsecutorsIndexFilterDto filter) {
        return auditCreate(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                () -> {
                    PrivateProsecutorsIndexFilterDto normalisedFilter =
                            reportFilterNormaliser.normalise(filter);
                    reportLocationValidator.validate(normalisedFilter.getLocation());
                    PrivateProsecutorsIndexReportLifecycle lifecycle;
                    try {
                        lifecycle = new PrivateProsecutorsIndexReportLifecycle();
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                "Unable to create private prosecutors index report output file", e);
                    }

                    var jobRequest = jobRequest(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT);

                    var response =
                            asyncJobService.startJob(
                                    jobRequest,
                                    new PrivateProsecutorsIndexReportDataReader(
                                            jdbcTemplate, normalisedFilter, schema),
                                    audited(lifecycle),
                                    reportPageSize);

                    JobAcknowledgement acknowledgement = jobMapper.toDto(response);
                    return new ReportJobCreation(
                            acknowledgement,
                            PrivateProsecutorsIndexReportParameterAudit.from(normalisedFilter));
                });
    }

    @Override
    public ReportDownload downloadReport(UUID jobId) {
        return auditService.processAudit(
                ReportAuditOperation.DOWNLOAD_REPORT_AUDIT_EVENT,
                unused -> {
                    JobStatusResponse jobStatusResponse = jobService.getJobStatusById(jobId);
                    if (jobStatusResponse.getStatus() != JobStatus1.COMPLETED
                            && jobStatusResponse.getType() != JobType.BULK_UPLOAD_ENTRIES) {
                        throw new AppRegistryException(
                                JobError.JOB_STATE_IS_NOT_SUITABLE_FOR_DOWNLOAD,
                                "Download stream not available");
                    }

                    try {
                        InputStreamResource resource = jobStatusResponse.read();
                        if (resource == null) {
                            throw noDownloadStream(null);
                        }

                        var reportDownload = new ReportDownload(REPORT_DOWNLOAD_FILENAME, resource);
                        return Optional.of(
                                new AuditableResult<>(
                                        reportDownload,
                                        ReportJobAudit.downloaded(
                                                jobStatusResponse,
                                                userProvider.getUserId(),
                                                reportDownload.filename())));
                    } catch (IOException e) {
                        throw noDownloadStream(e);
                    }
                });
    }

    private <T> AuditedReportLifecycle<T> audited(ReportCsvLifecycle<T> lifecycle) {
        return new AuditedReportLifecycle<>(lifecycle, reportJobAuditService);
    }

    private JobTypeRequest jobRequest(JobType jobType) {
        return JobTypeRequest.builder().jobType(jobType).userName(userProvider.getUserId()).build();
    }

    private ReportJobCreation auditCreate(
            ReportAuditOperation operation, ReportCreationSupplier supplier) {
        return auditService.processAudit(
                operation,
                unused -> {
                    ReportJobCreation reportJobCreation = supplier.get();
                    return Optional.of(
                            new AuditableResult<>(
                                    reportJobCreation,
                                    ReportJobAudit.created(
                                            reportJobCreation.acknowledgement(),
                                            userProvider.getUserId(),
                                            reportJobCreation.reportParameters())));
                });
    }

    private AppRegistryException noDownloadStream(Exception cause) {
        if (cause == null) {
            return new AppRegistryException(
                    JobError.JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM,
                    "Download stream not available");
        }
        return new AppRegistryException(
                JobError.JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM,
                "Download stream not available",
                cause);
    }

    @FunctionalInterface
    private interface ReportCreationSupplier {
        ReportJobCreation get();
    }
}
