package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.val;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkCreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.OfficialMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

@ExtendWith(OutputCaptureExtension.class)
class BulkUploadAsyncLifecycleTest {

    private BulkUploadAsyncLifecycle lifecycle;
    private BulkImportService bulkImportService;
    private BulkUploadApplicationEntryValidator bulkUploadApplicationEntryValidator;
    private BulkCreateApplicationEntryValidator bulkCreateApplicationEntryValidator;
    private BulkCreateApplicationEntryValidator.Session validationSession;
    private ApplicationList applicationList;
    private UUID listId;
    private MultipartFile csvFile;

    private static AsyncJobPersistenceService persistenceService;

    @BeforeEach
    void setUp() throws IOException {
        ApplicationListEntryMapperImpl mapper = new ApplicationListEntryMapperImpl();
        mapper.setApplicantMapper(new ApplicantMapperImpl());
        mapper.setOfficialMapper(new OfficialMapper());

        bulkImportService = mock(BulkImportService.class);
        bulkCreateApplicationEntryValidator = mock(BulkCreateApplicationEntryValidator.class);
        bulkUploadApplicationEntryValidator = mock(BulkUploadApplicationEntryValidator.class);
        validationSession = mock(BulkCreateApplicationEntryValidator.Session.class);
        applicationList = new ApplicationList();
        listId = UUID.randomUUID();
        csvFile = mock(MultipartFile.class);
        when(csvFile.getBytes())
                .thenReturn(new ByteArrayInputStream("HEADER\n".getBytes()).readAllBytes());
        persistenceService = mock(AsyncJobPersistenceService.class);

        doNothing().when(persistenceService).writeClob(any(), any());

        when(bulkCreateApplicationEntryValidator.createSession(applicationList))
                .thenReturn(validationSession);
        doAnswer(
                        invocation -> {
                            PayloadForCreate<EntryCreateDto> validatable =
                                    invocation.getArgument(0);
                            BiFunction<
                                            PayloadForCreate<EntryCreateDto>,
                                            CreateApplicationEntryValidationSuccess,
                                            ?>
                                    callback = invocation.getArgument(1);
                            return callback.apply(
                                    validatable,
                                    mock(CreateApplicationEntryValidationSuccess.class));
                        })
                .when(validationSession)
                .validate(any(), any());

        lifecycle =
                new BulkUploadAsyncLifecycle(
                        listId,
                        applicationList,
                        bulkImportService,
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
    void givenFieldCountMismatchAfterFinalRow_whenFailed_thenFormatsHeaderError()
            throws IOException {
        JobContext context = new JobContext();
        context.logFieldCountMismatch("Number of data fields does not match number of headers.");
        AsyncJobLifecycleEvent<BulkUploadRow> event =
                new AsyncJobLifecycleEvent<>(
                        new JobStatusResponse(
                                UUID.randomUUID(),
                                JobType.BULK_UPLOAD_ENTRIES,
                                JobStatus1.VALIDATING,
                                "user",
                                "error",
                                persistenceService),
                        null,
                        context,
                        JobStatus1.FAILED);

        lifecycle.setCSVFile(csvFile);
        lifecycle.failed(event);

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                -1,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                "Number of data fields does not match number of headers.",
                                                null,
                                                null,
                                                "HEADER_ERROR"))));
    }

    @Test
    void givenNonReaderFailure_whenFailed_thenPreservesExistingFailure() throws IOException {
        JobContext context = new JobContext();
        context.logFailure("Processing failed for row 2");
        AsyncJobLifecycleEvent<BulkUploadRow> event =
                new AsyncJobLifecycleEvent<>(null, null, context, JobStatus1.FAILED);

        lifecycle.failed(event);

        assertThat(context.getValidationFailureMessages())
                .containsExactly("Processing failed for row 2");
    }

    @Test
    void givenPostcodeViolatesOpenApiPattern_whenValidating_thenLogsBeanValidationFailure(
            CapturedOutput output) throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");
        JobContext context = new JobContext();
        final AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);
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

        // Respondent Test
        BulkUploadRow respondentRow = validRespondentRow();
        respondentRow.setRespondentPostcode("invalid");

        context = new JobContext();
        final AsyncJobLifecycleEvent<BulkUploadRow> event2 = event(respondentRow, context);

        lifecycle.setCSVFile(csvFile);
        exception = assertThrows(AppRegistryException.class, () -> lifecycle.validating(event2));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                3,
                                                "respondent.person.contactDetails.postcode",
                                                "invalid",
                                                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) "
                                                        + "\\d[A-Z]{2})|(GIR 0A{2}))$\"",
                                                row.getRespondentAddressLine1(),
                                                "John Doe",
                                                "DATA_ERROR"))));
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("\"rowNumber\":3")
                .contains("respondent.person.contactDetails.postcode")
                .contains("\"rejectedValue\":\"invalid\"")
                .contains("\"message\":\"must match")
                .contains("\"addressLine1\":\"1 Example Street\"")
                .contains("\"name\":\"John Doe\"")
                .contains("\"errorType\":\"DATA_ERROR\"");
    }

    @Test
    void
            givenPostcodeViolatesOpenApiPattern_whenValidatingRespondentWithMidddleName_thenLogsBeanValidationFailure(
                    CapturedOutput output) throws IOException {
        // Respondent Test
        BulkUploadRow respondentRow = validRespondentRow();
        respondentRow.setRespondentPostcode("invalid");
        respondentRow.setRespondentMiddleName("Middle");

        JobContext context = new JobContext();
        final AsyncJobLifecycleEvent<BulkUploadRow> event2 = event(respondentRow, context);

        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event2));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                2,
                                                "respondent.person.contactDetails.postcode",
                                                "invalid",
                                                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) "
                                                        + "\\d[A-Z]{2})|(GIR 0A{2}))$\"",
                                                respondentRow.getRespondentAddressLine1(),
                                                "John Middle Doe",
                                                "DATA_ERROR"))));
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("\"rowNumber\":2")
                .contains("respondent.person.contactDetails.postcode")
                .contains("\"rejectedValue\":\"invalid\"")
                .contains("\"message\":\"must match")
                .contains("\"addressLine1\":\"1 Example Street\"")
                .contains("\"name\":\"John Middle Doe\"")
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
        verify(validationSession).validate(any(), any());
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
        verify(validationSession).validate(any(), any());
    }

    @Test
    void givenCodeAwareValidationFailure_whenValidating_thenLogsRowFailure() throws IOException {
        BulkUploadRow row = validOrganisationRow();
        doThrow(
                        new AppRegistryException(
                                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                                "APPLICATION_TEXT1 is required for code AP99001"))
                .when(validationSession)
                .validate(any(), any());
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        verify(persistenceService, times(1)).writeClob(any(), any());

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
    void givenExistingValidationFailures_whenValidating_thenPrependsHeaderErrors()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");

        JobContext context = new JobContext();
        context.logFailure("first header validation failure");
        context.logFailure("second header validation failure");

        lifecycle.setCSVFile(csvFile);
        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(row, context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                -1,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                "second header validation failure",
                                                null,
                                                null,
                                                "HEADER_ERROR"),
                                        new BulkUploadError(
                                                -1,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                "first header validation failure",
                                                null,
                                                null,
                                                "HEADER_ERROR"),
                                        new BulkUploadError(
                                                2,
                                                "respondent.organisation.contactDetails.postcode",
                                                "invalid",
                                                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) "
                                                        + "\\d[A-Z]{2})|(GIR 0A{2}))$\"",
                                                row.getRespondentAddressLine1(),
                                                row.getRespondentOrganisationName(),
                                                "DATA_ERROR"))));
    }

    @Test
    void givenUnknownBusinessRuleFailureForPersonRespondent_whenValidating_thenUsesMiddleName()
            throws IOException {
        BulkUploadRow row = validRespondentRow();
        row.setRespondentMiddleName("Byron");
        doThrow(
                        new AppRegistryException(
                                CommonAppError.INTERNAL_SERVER_ERROR,
                                "Unexpected validation failure"))
                .when(validationSession)
                .validate(any(), any());
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);
        lifecycle.setCSVFile(csvFile);

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
                                                "John Byron Doe",
                                                "DATA_ERROR"))));

        BulkUploadRow respondentRow = validRespondentRow();
        doThrow(
                        new AppRegistryException(
                                CommonAppError.INTERNAL_SERVER_ERROR,
                                "Unexpected validation failure"))
                .when(validationSession)
                .validate(any(), any());
        context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event2 = event(respondentRow, context);

        lifecycle.setCSVFile(csvFile);
        exception = assertThrows(AppRegistryException.class, () -> lifecycle.validating(event2));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                3,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                "Unexpected validation failure",
                                                row.getRespondentAddressLine1(),
                                                "John Doe",
                                                "DATA_ERROR"))));
    }

    @Test
    void givenPagePersistenceFailure_whenProcessing_thenFailsJobWithPageRowNumber()
            throws IOException {
        BulkUploadRow firstRow = validOrganisationRow();
        BulkUploadRow secondRow = validOrganisationRow();
        JobContext context = new JobContext();
        lifecycle.validating(
                new AsyncJobLifecycleEvent<>(
                        null, List.of(firstRow, secondRow), context, JobStatus1.VALIDATING));
        AsyncJobLifecycleEvent<BulkUploadRow> event =
                new AsyncJobLifecycleEvent<>(
                        null, List.of(firstRow, secondRow), context, JobStatus1.PROCESSING);
        when(bulkImportService.persistPage(any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.processing(event));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED);
        assertThat(context.getValidationFailureMessages())
                .containsExactly("Processing failed for page starting at row 2: boom");
        verify(bulkImportService).persistPage(any(), any());
    }

    @Test
    void
            givenDuplicateRowsAcrossPage_whenProcessingAndCompleting_thenPreservesCountsAndLogsDuration(
                    CapturedOutput output) throws IOException {
        UUID jobId = UUID.randomUUID();
        var response =
                JobStatusResponse.builder()
                        .uuid(jobId)
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus1.PROCESSING)
                        .userName("user")
                        .build();
        var duplicate = validOrganisationRow();
        var context = new JobContext();
        lifecycle.received(
                new AsyncJobLifecycleEvent<>(response, null, context, JobStatus1.RECEIVED));
        lifecycle.validating(
                new AsyncJobLifecycleEvent<>(
                        response, List.of(duplicate, duplicate), context, JobStatus1.VALIDATING));
        when(bulkImportService.persistPage(eq(jobId), any())).thenReturn(2);

        lifecycle.processing(
                new AsyncJobLifecycleEvent<>(
                        response, List.of(duplicate, duplicate), context, JobStatus1.PROCESSING));
        lifecycle.completed(
                new AsyncJobLifecycleEvent<>(response, null, context, JobStatus1.COMPLETED));

        var pageCaptor = ArgumentCaptor.<List<ValidatedBulkImportEntry>>captor();
        verify(bulkImportService).persistPage(eq(jobId), pageCaptor.capture());
        assertThat(pageCaptor.getValue())
                .hasSize(2)
                .extracting(ValidatedBulkImportEntry::rowNumber)
                .containsExactly(2, 3);
        verify(bulkImportService).completed(listId, jobId, 2);
        assertThat(output)
                .contains(
                        "Bulk upload completed listId="
                                + listId
                                + " jobId="
                                + jobId
                                + " importedEntryCount=2 durationMs=");
    }

    @Test
    void givenMultipleProcessingPages_thenPreservesCsvRowNumbers() throws IOException {
        UUID jobId = UUID.randomUUID();
        var response =
                JobStatusResponse.builder()
                        .uuid(jobId)
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus1.PROCESSING)
                        .userName("user")
                        .build();
        var context = new JobContext();
        var rows = List.of(validOrganisationRow(), validOrganisationRow(), validOrganisationRow());
        lifecycle.validating(
                new AsyncJobLifecycleEvent<>(response, rows, context, JobStatus1.VALIDATING));
        when(bulkImportService.persistPage(eq(jobId), any()))
                .thenAnswer(invocation -> invocation.<List<?>>getArgument(1).size());

        lifecycle.processing(
                new AsyncJobLifecycleEvent<>(
                        response, rows.subList(0, 2), context, JobStatus1.PROCESSING));
        lifecycle.processing(
                new AsyncJobLifecycleEvent<>(
                        response, rows.subList(2, 3), context, JobStatus1.PROCESSING));

        var pageCaptor = ArgumentCaptor.<List<ValidatedBulkImportEntry>>captor();
        verify(bulkImportService, times(2)).persistPage(eq(jobId), pageCaptor.capture());
        assertThat(pageCaptor.getAllValues())
                .flatExtracting(entries -> entries)
                .extracting(ValidatedBulkImportEntry::rowNumber)
                .containsExactly(2, 3, 4);
    }

    @Test
    void givenUnableToConvertErrorToJson_thenThrowsJsonProcessingError(CapturedOutput output)
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");
        JobContext context = new JobContext();
        lifecycle.setCSVFile(csvFile);

        try (MockedConstruction<ObjectMapper> ignored =
                mockConstruction(
                        ObjectMapper.class,
                        (mock, constructionContext) ->
                                when(mock.writeValueAsString(any()))
                                        .thenThrow(new JsonProcessingException("boom") {}))) {

            AppRegistryException exception =
                    assertThrows(
                            AppRegistryException.class,
                            () -> lifecycle.validating(event(row, context)));

            assertThat(exception.getCode())
                    .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        }

        assertThat(context.getValidationFailureMessages())
                .containsExactly("Bulk upload validation failure for list " + listId + ": boom");

        assertThat(output)
                .contains("Failed to serialize bulk upload errors to JSON for list")
                .contains(listId.toString())
                .contains("boom");
    }

    @Test
    void givenTooManyColumnsInCsv_whenValidating_thenContextErrorContainsHeaderError()
            throws IOException {
        String headerErrorMessage = "Too many columns in CSV file";
        JobContext context = new JobContext();
        context.logFieldCountMismatch(headerErrorMessage);

        BulkUploadRow row = validOrganisationRow();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);
        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                -1,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                headerErrorMessage,
                                                null,
                                                null,
                                                "HEADER_ERROR"))));

        lifecycle.failed(new AsyncJobLifecycleEvent<>(null, null, context, JobStatus1.FAILED));

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                -1,
                                                "BULK_UPLOAD_ROW",
                                                null,
                                                headerErrorMessage,
                                                null,
                                                null,
                                                "HEADER_ERROR"))));

        verify(validationSession).validate(any(), any());
        verify(persistenceService).writeClob(any(), any());
    }

    @Test
    void whenValidating_csvFileIsSet_thenWritesClobSuccessfully() throws IOException {
        when(csvFile.getBytes()).thenReturn("HEADER\nrow-two\nrow-three\n".getBytes());

        StringBuilder writtenCsv = new StringBuilder();
        doAnswer(
                        invocation -> {
                            ByteArrayInputStream inputStream = invocation.getArgument(1);
                            writtenCsv.append(new String(inputStream.readAllBytes()));
                            return null;
                        })
                .when(persistenceService)
                .writeClob(any(), any());

        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");

        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(writtenCsv.toString())
                .contains("HEADER|")
                .contains("row-two|must match")
                .contains("row-three|");

        verify(persistenceService, times(1)).writeClob(any(), any());
    }

    @Test
    void whenValidating_csvFileIsSet_containsHeaderErrors_thenWritesClobSuccessfully()
            throws IOException {
        when(csvFile.getBytes()).thenReturn("HEADER\nrow-two\nrow-three\n".getBytes());

        StringBuilder writtenCsv = new StringBuilder();
        doAnswer(
                        invocation -> {
                            ByteArrayInputStream inputStream = invocation.getArgument(1);
                            writtenCsv.append(new String(inputStream.readAllBytes()));
                            return null;
                        })
                .when(persistenceService)
                .writeClob(any(), any());

        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("invalid");

        JobContext context = new JobContext();
        context.setValidationFailureMessages(
                List.of("HEADER_ERROR:1", "HEADER_ERROR:2", "HEADER_ERROR:3", "HEADER_ERROR:4"));
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(writtenCsv.toString())
                .contains("HEADER|HEADER_ERROR:4|HEADER_ERROR:3|HEADER_ERROR:2|HEADER_ERROR:1")
                .contains("row-two|must match")
                .contains("row-three|");

        verify(persistenceService, times(1)).writeClob(any(), any());
    }

    @Test
    void whenValidating_csvFileNotSet_throwsException() throws IOException {
        JobContext context = new JobContext();
        context.logFieldCountMismatch("HEADER MISMATCH");

        val row = validOrganisationRow();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception).isNotNull();
        verify(persistenceService, times(0)).writeClob(any(), any());

        assertThat(exception.getMessage()).contains("Failed to save error CSV");
    }

    @Test
    void whenValidating_csvFileDoesNotExist_throwsException() throws IOException {
        JobContext context = new JobContext();
        context.logFieldCountMismatch("HEADER MISMATCH");

        val row = validOrganisationRow();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);
        doThrow(new IOException("write failed")).when(persistenceService).writeClob(any(), any());

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception).isNotNull();

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(exception.getMessage())
                .contains("Failed to save error CSV")
                .contains("write failed");

        verify(persistenceService, times(1)).writeClob(any(), any());

        assertThat(exception.getMessage()).contains("Failed to save error CSV");
    }

    private static AsyncJobLifecycleEvent<BulkUploadRow> event(
            BulkUploadRow row, JobContext context) {
        return new AsyncJobLifecycleEvent<>(
                new JobStatusResponse(
                        UUID.randomUUID(),
                        JobType.BULK_UPLOAD_ENTRIES,
                        JobStatus1.VALIDATING,
                        "user",
                        "error",
                        persistenceService),
                List.of(row),
                context,
                JobStatus1.VALIDATING);
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

    private static BulkUploadRow validRespondentRow() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("AP99001");
        row.setRespondentFirstName("John");
        row.setRespondentLastName("Doe");
        row.setRespondentAddressLine1("1 Example Street");
        row.setRespondentPostcode("AA1 1AA");
        row.setRespondentEmail("example.respondent@example.com");
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
