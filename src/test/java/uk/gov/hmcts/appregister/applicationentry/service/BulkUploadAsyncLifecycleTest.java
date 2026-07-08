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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
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
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                2,
                                                "respondent.organisation.contactDetails.postcode",
                                                "invalid",
                                                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) "
                                                        + "\\d[A-Z]{2})|(GIR 0A{2}))$\"",
                                                row.getRespondentAddressLine1(),
                                                row.getRespondentOrganisationName(),
                                                "DATA_ERROR"))));
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("\"rowNumber\":2")
                .contains("respondent.organisation.contactDetails.postcode")
                .contains("\"rejectedValue\":\"invalid\"")
                .contains("\"message\":\"must match")
                .contains("\"addressLine1\":\"1 Example Street\"")
                .contains("\"name\":\"Example Organisation\"")
                .contains("\"errorType\":\"DATA_ERROR\"");
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
                .isEqualTo(AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        "[APPLICATION_LIST]: The application list does not exist %s"
                                .formatted(listId));
        assertThat(context.getValidationFailureMessages().getFirst()).doesNotContain("Row ");
        verify(bulkCreateApplicationEntryValidator, never()).validate(any(), any());
    }

    @Test
    void givenApplicationListStateIsIncorrect_whenValidating_thenThrowsListStateError() {
        String invalidStateMessage =
                "The application list id %s is not in the correct state or the application list is deleted CLOSED"
                        .formatted(listId);
        doThrow(
                        new AppRegistryException(
                                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                                invalidStateMessage))
                .when(bulkCreateApplicationEntryValidator)
                .validateApplicationList(listId);
        JobContext context = new JobContext();

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(validOrganisationRow(), context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT);
        assertThat(context.getValidationFailureMessages())
                .containsExactly("[APPLICATION_LIST]: " + invalidStateMessage);
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
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                2,
                                                "APPLICATION_TEXT",
                                                null,
                                                "APPLICATION_TEXT1 is required for code AP99001",
                                                row.getRespondentAddressLine1(),
                                                row.getRespondentOrganisationName(),
                                                "DATA_ERROR"))));
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

        String name =
                row.getRespondentOrganisationName() != null
                        ? row.getRespondentOrganisationName()
                        : row.getRespondentForename1() + " " + row.getRespondentSurname();

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                2,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                "Unexpected validation failure",
                                                row.getRespondentAddressLine1(),
                                                name,
                                                "DATA_ERROR"))));
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

    private static String createErrorDescription(List<BulkUploadError> errors) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize errors", e);
        }
    }
}
