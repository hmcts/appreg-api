package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import jakarta.validation.Validation;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

@ExtendWith(OutputCaptureExtension.class)
class BulkUploadAsyncLifecycleTest {

    private BulkUploadAsyncLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        ApplicationListEntryMapperImpl mapper = new ApplicationListEntryMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setOfficialMapper(new OfficialMapper());

        lifecycle =
                new BulkUploadAsyncLifecycle(
                        UUID.randomUUID(),
                        mock(ApplicationEntryService.class),
                        new BulkUploadApplicationEntryValidator(),
                        mapper,
                        Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void givenPostcodeViolatesOpenApiPattern_whenValidating_thenLogsBeanValidationFailure(
            CapturedOutput output) throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");
        JobContext context = new JobContext();

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(row, context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .anySatisfy(
                        message -> {
                            assertThat(message)
                                    .contains("Row 2 [respondent.organisation.contactDetails.postcode]");
                            assertThat(message).contains("must match");
                        });
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("Row 2 [respondent.organisation.contactDetails.postcode]")
                .contains("must match");
    }

    @Test
    void givenMobileAllowedByOpenApiPattern_whenValidating_thenValidationPasses()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentMobile("+44 7700 900123");
        JobContext context = new JobContext();

        lifecycle.validating(event(row, context));

        assertThat(context.hasFailure()).isFalse();
    }

    @Test
    void givenOverlengthFields_whenValidating_thenValidationPassesAfterTruncation()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentOrganisationName("O".repeat(105));
        row.setRespondentAddressLine1("1".repeat(40));
        row.setRespondentPostcode("SW1A 2AAZZZ");
        row.setRespondentTelephone("1".repeat(25));
        row.setRespondentMobile("2".repeat(25));
        JobContext context = new JobContext();

        lifecycle.validating(event(row, context));

        assertThat(context.hasFailure()).isFalse();
    }

    private static AsyncJobLifecycleEvent<BulkUploadRow> event(
            BulkUploadRow row, JobContext context) {
        return new AsyncJobLifecycleEvent<>(null, List.of(row), context, JobStatus1.VALIDATING);
    }

    private static BulkUploadRow validOrganisationRow() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("AP99001");
        row.setRespondentOrganisationName("Example Organisation");
        row.setRespondentAddressLine1("1 Example Street");
        row.setRespondentPostcode("AA1 1AA");
        row.setRespondentEmail("example.organisation@example.com");
        row.setRespondentTelephone("0207 1111111");
        row.setRespondentMobile("07771 111111");
        return row;
    }
}
