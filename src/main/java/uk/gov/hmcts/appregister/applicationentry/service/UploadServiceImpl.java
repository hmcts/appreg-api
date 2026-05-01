package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.validation.Validator;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.job.BulkUploadAsyncLifecycle;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.service.JobService;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final BulkUploadApplicationEntryValidator bulkUploadApplicationEntryValidator;
    private final AsyncJobService asyncJobService;
    private final JobService jobService;
    private final UserProvider userProvider;
    private final Validator beanValidator;
    private final ApplicationListEntryMapper applicationListEntryMapper;
    private final ApplicationEntryService applicationEntryService;

    @Override
    public JobAcknowledgement uploadAppEntryCsv(UUID listId, MultipartFile file)
            throws IOException {
        JobTypeRequest jobTypeRequest =
                JobTypeRequest.builder()
                        .userName(userProvider.getUserId())
                        .jobType(JobType.BULK_UPLOAD_ENTRIES)
                        .build();

        CsvReader<BulkUploadRow> csvReader = new CsvReader<>(file, BulkUploadRow.class);

        TrackJobStatusResponse trackJobStatusResponse =
                asyncJobService.startJob(
                        jobTypeRequest,
                        csvReader,
                        new BulkUploadAsyncLifecycle(
                                listId,
                                applicationEntryService,
                                bulkUploadApplicationEntryValidator,
                                applicationListEntryMapper,
                                beanValidator));

        return jobService.getJobAckById(trackJobStatusResponse.getUuid());
    }
}
