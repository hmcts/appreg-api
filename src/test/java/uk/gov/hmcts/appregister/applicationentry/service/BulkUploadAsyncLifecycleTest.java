package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkCreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

@ExtendWith(OutputCaptureExtension.class)
class BulkUploadAsyncLifecycleTest {

    private BulkUploadAsyncLifecycle lifecycle;
    private ApplicationEntryService applicationEntryService;
    private BulkCreateApplicationEntryValidator bulkCreateApplicationEntryValidator;
    private UUID listId;

    @BeforeEach
    void setUp() {
        ApplicationListEntryMapperImpl mapper = new ApplicationListEntryMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setOfficialMapper(new OfficialMapper());

        applicationEntryService = mock(ApplicationEntryService.class);
        bulkCreateApplicationEntryValidator = mock(BulkCreateApplicationEntryValidator.class);
        listId = UUID.randomUUID();

        lifecycle =
                new BulkUploadAsyncLifecycle(
                        listId,
                        applicationEntryService,
                        new BulkUploadApplicationEntryValidator(),
                        bulkCreateApplicationEntryValidator,
                        mapper,
                        Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void givenNoRows_whenValidating_thenThrowsEmptyFileError() {
        val event =
                new AsyncJobLifecycleEvent<BulkUploadRow>(
                        null, List.of(), new JobContext(), JobStatus1.VALIDATING);

        val exception = assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_UPLOAD_EMPTY_FILE);
    }

    @Test
    void givenPostcodeViolatesOpenApiPattern_whenValidating_thenLogsBeanValidationFailure(
            CapturedOutput output) {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .anySatisfy(
                        message -> {
                            assertThat(message)
                                    .contains(
                                            "Row 2 [respondent.organisation.contactDetails.postcode]");
                            assertThat(message).contains("rejected value [invalid]");
                            assertThat(message).contains("must match");
                        });
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("Row 2 [respondent.organisation.contactDetails.postcode]")
                .contains("rejected value [invalid]")
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
        verify(bulkCreateApplicationEntryValidator).validateApplicationList(listId);
        verify(bulkCreateApplicationEntryValidator).validate(any(), any());
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

    @Test
    void givenApplicationListDoesNotExist_whenValidatingMultipleRows_thenLogsListFailureOnce() {
        doThrow(
                        new AppRegistryException(
                                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                                "The application list does not exist %s".formatted(listId)))
                .when(bulkCreateApplicationEntryValidator)
                .validateApplicationList(listId);
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event =
                new AsyncJobLifecycleEvent<>(
                        null,
                        List.of(validOrganisationRow(), validOrganisationRow()),
                        context,
                        JobStatus1.VALIDATING);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        "[APPLICATION_LIST]: The application list does not exist %s"
                                .formatted(listId));
        assertThat(context.getValidationFailureMessages().getFirst()).doesNotContain("Row ");
        verify(bulkCreateApplicationEntryValidator, never()).validate(any(), any());
    }

    @Test
    void givenBlankApplicationText_whenValidating_thenDefersToCodeAwareValidation()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        var applicationTexts = new ArrayListValuedHashMap<String, String>();
        applicationTexts.put("APPLICATION_TEXT1", "");
        applicationTexts.put("APPLICATION_TEXT2", "");
        row.setApplicationTexts(applicationTexts);
        JobContext context = new JobContext();

        lifecycle.validating(event(row, context));

        assertThat(context.hasFailure()).isFalse();
        verify(bulkCreateApplicationEntryValidator).validate(any(), any());
    }

    @Test
    void givenCodeAwareValidationFailure_whenValidating_thenLogsRowFailure() {
        BulkUploadRow row = validOrganisationRow();
        doThrow(
                        new AppRegistryException(
                                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                                "APPLICATION_TEXT1 is required for code AP99001"))
                .when(bulkCreateApplicationEntryValidator)
                .validate(any(), any());
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        "Row 2 [APPLICATION_TEXT]: APPLICATION_TEXT1 is required for code AP99001");
    }

    @Test
    void givenUnknownBusinessRuleFailure_whenValidating_thenUsesGenericRowLocation() {
        BulkUploadRow row = validOrganisationRow();
        doThrow(
                        new AppRegistryException(
                                CommonAppError.INTERNAL_SERVER_ERROR,
                                "Unexpected validation failure"))
                .when(bulkCreateApplicationEntryValidator)
                .validate(any(), any());
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly("Row 2 [BULK_UPLOAD_ROW]: Unexpected validation failure");
    }

    @Test
    void givenProcessingFailureOnSecondRow_whenProcessing_thenStopsAtFailureAndLogsRowNumber() {
        BulkUploadRow firstRow = validOrganisationRow();
        BulkUploadRow secondRow = validOrganisationRow();
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event =
                new AsyncJobLifecycleEvent<>(
                        null, List.of(firstRow, secondRow), context, JobStatus1.PROCESSING);
        when(applicationEntryService.createBulkEntry(any()))
                .thenReturn(null)
                .thenThrow(new IllegalStateException("boom"));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.processing(event));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly("Processing failed for row 3: boom");
        verify(applicationEntryService, times(2)).createBulkEntry(any());
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
