package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.report.audit.ActivityAuditReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.DurationReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.FeesReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ListMaintenanceReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.PrivateProsecutorsIndexReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ReportJobAuditService;
import uk.gov.hmcts.appregister.report.audit.SearchWarrantsReportParameterAudit;
import uk.gov.hmcts.appregister.report.validator.ReportLocationValidator;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final AsyncJobService asyncJobService;
    private final UserProvider userProvider;
    private final JobMapper jobMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ReportJobAuditService reportJobAuditService;
    private final ReportFilterNormaliser reportFilterNormaliser;
    private final ReportLocationValidator reportLocationValidator;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Value("${appreg.report.page-size}")
    private int reportPageSize;

    @Override
    public ReportJobCreation createActivityAuditReport(ActivityAuditFilterDto filter) {
        ActivityAuditFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
        ActivityAuditReportLifecycle lifecycle;
        try {
            lifecycle = new ActivityAuditReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create activity audit report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.ACTIVITY_AUDIT_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new ActivityAuditReportDataReader(jdbcTemplate, normalisedFilter, schema),
                        audited(lifecycle),
                        reportPageSize);

        JobAcknowledgement acknowledgement = jobMapper.toDto(response);
        return new ReportJobCreation(
                acknowledgement, ActivityAuditReportParameterAudit.from(normalisedFilter));
    }

    @Override
    public ReportJobCreation createFeesReport(FeesReportFilterDto filter) {
        FeesReportFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
        reportLocationValidator.validate(normalisedFilter.getLocation());
        FeesReportLifecycle lifecycle;
        try {
            lifecycle = new FeesReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create fees report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new FeesReportDataReader(jdbcTemplate, normalisedFilter, schema),
                        audited(lifecycle),
                        reportPageSize);

        JobAcknowledgement acknowledgement = jobMapper.toDto(response);
        return new ReportJobCreation(
                acknowledgement, FeesReportParameterAudit.from(normalisedFilter));
    }

    @Override
    public ReportJobCreation createSearchWarrantsReport(SearchWarrantsReportFilterDto filter) {
        SearchWarrantsReportFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
        reportLocationValidator.validate(normalisedFilter.getLocation());
        SearchWarrantsReportLifecycle lifecycle;

        try {
            lifecycle = new SearchWarrantsReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create search warrant report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.SEARCH_WARRANTS_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new SearchWarrantsReportDataReader(jdbcTemplate, normalisedFilter, schema),
                        audited(lifecycle),
                        reportPageSize);

        JobAcknowledgement acknowledgement = jobMapper.toDto(response);
        return new ReportJobCreation(
                acknowledgement, SearchWarrantsReportParameterAudit.from(normalisedFilter));
    }

    @Override
    public ReportJobCreation createDurationReport(DurationFilterDto filter) {
        DurationFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
        reportLocationValidator.validate(normalisedFilter.getLocation());
        DurationReportLifecycle lifecycle;
        try {
            lifecycle = new DurationReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create duration report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.DURATION_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new DurationReportDataReader(jdbcTemplate, normalisedFilter, schema),
                        audited(lifecycle),
                        reportPageSize);

        JobAcknowledgement acknowledgement = jobMapper.toDto(response);
        return new ReportJobCreation(
                acknowledgement, DurationReportParameterAudit.from(normalisedFilter));
    }

    @Override
    public ReportJobCreation createListMaintenanceReport(ListMaintenanceFilterDto filter) {
        ListMaintenanceFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
        reportLocationValidator.validate(normalisedFilter.getLocation());
        ListMaintenanceReportLifecycle lifecycle;
        try {
            lifecycle = new ListMaintenanceReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create list maintenance report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.LIST_MAINTENANCE_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new ListMaintenanceReportDataReader(jdbcTemplate, normalisedFilter, schema),
                        audited(lifecycle),
                        reportPageSize);

        JobAcknowledgement acknowledgement = jobMapper.toDto(response);
        return new ReportJobCreation(
                acknowledgement, ListMaintenanceReportParameterAudit.from(normalisedFilter));
    }

    @Override
    public ReportJobCreation createPrivateProsecutorsIndexReport(
            PrivateProsecutorsIndexFilterDto filter) {
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

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

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
    }

    private <T> AuditedReportLifecycle<T> audited(ReportCsvLifecycle<T> lifecycle) {
        return new AuditedReportLifecycle<>(lifecycle, reportJobAuditService);
    }
}
