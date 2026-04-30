package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.Location;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.report.exception.ReportError;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final AsyncJobService asyncJobService;
    private final UserProvider userProvider;
    private final JobMapper jobMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Value("${appreg.report.page-size}")
    private int reportPageSize;

    @Override
    public JobAcknowledgement createActivityAuditReport(ActivityAuditFilterDto filter) {
        normaliseDateRange(filter);

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
                        new ActivityAuditReportDataReader(jdbcTemplate, filter, schema),
                        lifecycle,
                        reportPageSize);

        return jobMapper.toDto(response);
    }

    @Override
    public JobAcknowledgement createFeesReport(FeesReportFilterDto filter) {
        normaliseDateRange(filter);

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
                        new FeesReportDataReader(jdbcTemplate, filter, schema),
                        lifecycle,
                        reportPageSize);

        return jobMapper.toDto(response);
    }

    @Override
    public JobAcknowledgement createDurationReport(DurationFilterDto filter) {
        normaliseDateRange(filter);
        validateDurationLocation(filter);

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
                        new DurationReportDataReader(jdbcTemplate, filter, schema),
                        lifecycle,
                        reportPageSize);

        return jobMapper.toDto(response);
    }

    private void normaliseDateRange(ActivityAuditFilterDto filter) {
        normaliseDateRange(
                filter::getDateFrom, filter::getDateTo, filter::setDateFrom, filter::setDateTo);
    }

    private void normaliseDateRange(FeesReportFilterDto filter) {
        normaliseDateRange(
                filter::getDateFrom, filter::getDateTo, filter::setDateFrom, filter::setDateTo);
    }

    private void normaliseDateRange(DurationFilterDto filter) {
        normaliseDateRange(
                filter::getDateFrom, filter::getDateTo, filter::setDateFrom, filter::setDateTo);
    }

    private void normaliseDateRange(
            Supplier<LocalDate> dateFromSupplier,
            Supplier<LocalDate> dateToSupplier,
            Consumer<LocalDate> dateFromSetter,
            Consumer<LocalDate> dateToSetter) {
        LocalDate dateFrom = dateFromSupplier.get();
        LocalDate dateTo = dateToSupplier.get();

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            dateFromSetter.accept(dateTo);
            dateToSetter.accept(dateFrom);
        }
    }

    private void validateDurationLocation(DurationFilterDto filter) {
        Location location = filter.getLocation();
        if (location == null) {
            return;
        }

        boolean hasCourt = StringUtils.hasText(location.getCourtLocationCode());
        boolean hasOtherLocation = StringUtils.hasText(location.getOtherLocationDescription());
        boolean hasCja = StringUtils.hasText(location.getCjaCode());

        if (hasCourt && (hasOtherLocation || hasCja)) {
            throw invalidDurationLocation();
        }
    }

    private AppRegistryException invalidDurationLocation() {
        return new AppRegistryException(
                ReportError.INVALID_LOCATION_COMBINATION,
                "Provide no location filter, courtLocationCode only, otherLocationDescription only, "
                        + "cjaCode only, or cjaCode with otherLocationDescription.");
    }
}
