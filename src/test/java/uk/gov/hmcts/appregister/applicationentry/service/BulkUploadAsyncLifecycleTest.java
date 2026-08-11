package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import java.util.concurrent.atomic.AtomicInteger;
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
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@ExtendWith(OutputCaptureExtension.class)
class BulkUploadAsyncLifecycleTest {

    private BulkUploadAsyncLifecycle lifecycle;
    private BulkImportService bulkImportService;
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
                            validatable.getData().setWordingFields(List.of());
                            BiFunction<
                                            PayloadForCreate<EntryCreateDto>,
                                            CreateApplicationEntryValidationSuccess,
                                            ?>
                                    callback = invocation.getArgument(1);
                            return callback.apply(validatable, validationResult("Wording"));
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
                        null, List.of(), new JobContext(), JobStatus.VALIDATING);

        val exception = assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_UPLOAD_EMPTY_FILE);
    }

    @Test
    void givenNonEnforcementApplicationWithBlankAccountNumber_whenValidating_thenSucceeds()
            throws IOException {
        var row = validOrganisationRow();
        row.setApplicationCode("SW99063");
        row.setAccountNumber("");

        lifecycle.validating(event(row, new JobContext()));

        verify(validationSession)
                .validate(argThat(payload -> payload.getData().getAccountNumber() == null), any());
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
                                JobStatus.VALIDATING,
                                "user",
                                "error",
                                persistenceService),
                        null,
                        context,
                        JobStatus.FAILED);

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
                new AsyncJobLifecycleEvent<>(null, null, context, JobStatus.FAILED);

        lifecycle.failed(event);

        assertThat(context.getValidationFailureMessages())
                .containsExactly("Processing failed for row 2");
    }

    @Test
    void givenUnexpectedFailureWithoutClientDetails_whenFailed_thenAddsSafeJobReference()
            throws IOException {
        UUID jobId = UUID.randomUUID();
        JobContext context = new JobContext();
        var response =
                JobStatusResponse.builder()
                        .uuid(jobId)
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus.FAILED)
                        .userName("user")
                        .build();

        lifecycle.failed(new AsyncJobLifecycleEvent<>(response, null, context, JobStatus.FAILED));

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        "Bulk upload processing failed. Contact support quoting job reference "
                                + jobId
                                + ".");
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
                                                "postcode",
                                                "invalid",
                                                "Provide a valid UK postcode.",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"))));
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("\"rowNumber\":2")
                .contains("postcode")
                .contains("\"rejectedValue\":\"invalid\"")
                .contains("\"message\":\"Provide a valid UK postcode.\"")
                .contains("\"addressLine1\":\"1 Example Street\"")
                .contains("\"code\":\"" + row.getApplicantCode() + "\"")
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
                                                "postcode",
                                                "invalid",
                                                "Provide a valid UK postcode.",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"))));
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("\"rowNumber\":3")
                .contains("postcode")
                .contains("\"rejectedValue\":\"invalid\"")
                .contains("\"message\":\"Provide a valid UK postcode.\"")
                .contains("\"addressLine1\":\"1 Example Street\"")
                .contains("\"code\":\"" + row.getApplicantCode() + "\"")
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
                                                "postcode",
                                                "invalid",
                                                "Provide a valid UK postcode.",
                                                respondentRow.getRespondentAddressLine1(),
                                                respondentRow.getApplicantCode(),
                                                "DATA_ERROR"))));
        assertThat(output)
                .contains("Bulk upload validation failure for list")
                .contains("\"rowNumber\":2")
                .contains("postcode")
                .contains("\"rejectedValue\":\"invalid\"")
                .contains("\"message\":\"Provide a valid UK postcode.\"")
                .contains("\"addressLine1\":\"1 Example Street\"")
                .contains("\"code\":\"APP001\"")
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
    void givenPostcodeViolatesPatternAndSize_whenValidating_thenLogsOneFriendlyError()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentPostcode("INVALID99");

        assertFriendlyContactValidationFailure(
                row, "postcode", "INVALID99", "Provide a valid UK postcode.");
    }

    @Test
    void givenTelephoneViolatesPatternAndSize_whenValidating_thenLogsOneFriendlyError()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentTelephone("invalid");

        assertFriendlyContactValidationFailure(
                row, "phone", "invalid", "Provide a valid UK telephone number.");
    }

    @Test
    void givenMobileViolatesPatternAndSize_whenValidating_thenLogsOneFriendlyError()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentMobile("invalid");

        assertFriendlyContactValidationFailure(
                row, "mobile", "invalid", "Provide a valid UK mobile number.");
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
                                                row.getApplicantCode(),
                                                "DATA_ERROR"))));
    }

    @Test
    void givenMissingRequiredFields_whenValidatingBusinessRules_thenLogsRowFailure()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setApplicationCode(null);
        row.setApplicantCode(null);
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        verify(persistenceService, times(1)).writeClob(any(), any());

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        ObjectMapper mapper = new ObjectMapper();
        BulkUploadError[] errors =
                mapper.readValue(
                        context.getValidationFailureMessages().getFirst(), BulkUploadError[].class);
        assertThat(errors).hasSize(3);

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        createErrorDescription(
                                List.of(
                                        new BulkUploadError(
                                                2,
                                                "standardApplicantCode",
                                                null,
                                                "Applicant code is required",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"),
                                        new BulkUploadError(
                                                2,
                                                "applicationCode",
                                                null,
                                                "Application code is required",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"),
                                        new BulkUploadError(
                                                2,
                                                "applicationCode",
                                                null,
                                                "must not be null",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"))));
    }

    @Test
    void givenMissingRespondentNames_whenValidating_thenLogsOnlyBusinessFriendlyNameError()
            throws IOException {
        BulkUploadRow row = validRespondentRow();
        row.setRespondentTitle("");
        row.setRespondentFirstName(null);
        row.setRespondentLastName(null);
        JobContext context = new JobContext();
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
                                                2,
                                                "RESPONDENT",
                                                null,
                                                "Respondent details are missing. Enter either"
                                                        + " Organisation Name, or Respondent First"
                                                        + " Name and Last Name.",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"))))
                .allSatisfy(
                        errorDescription ->
                                assertThat(errorDescription)
                                        .doesNotContain("must not be null")
                                        .doesNotContain("size must be between")
                                        .doesNotContain("respondent.person.name"));
    }

    @Test
    void givenPartialPersonName_whenValidating_thenRetainsRelevantLastNameError()
            throws IOException {
        BulkUploadRow row = validRespondentRow();
        row.setRespondentLastName(null);
        JobContext context = new JobContext();
        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(row, context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages().getFirst())
                .contains("lastName")
                .contains("must not be null")
                .doesNotContain("Respondent details are missing");
    }

    @Test
    void givenMissingRespondentNamesAndInvalidContactField_whenValidating_thenRetainsContactError()
            throws IOException {
        BulkUploadRow row = validRespondentRow();
        row.setRespondentFirstName(null);
        row.setRespondentLastName(null);
        row.setRespondentPostcode("invalid");
        JobContext context = new JobContext();
        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(row, context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        String errorDescription = context.getValidationFailureMessages().getFirst();
        assertThat(errorDescription)
                .contains("Respondent details are missing")
                .contains("postcode")
                .doesNotContain("respondent.person.name");
    }

    @Test
    void givenOrganisationAndPersonNames_whenValidating_thenLogsMutualExclusionError()
            throws IOException {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentFirstName("Jane");
        row.setRespondentLastName("Jones");
        JobContext context = new JobContext();
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
                                                2,
                                                "RESPONDENT",
                                                null,
                                                "Respondent cannot be both organisation and person",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
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
        var validationEvent = event(row, context);
        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class, () -> lifecycle.validating(validationEvent));

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
                                                "postcode",
                                                "invalid",
                                                "Provide a valid UK postcode.",
                                                row.getRespondentAddressLine1(),
                                                row.getApplicantCode(),
                                                "DATA_ERROR"))));
    }

    @Test
    void givenUnknownBusinessRuleFailure_whenValidating_thenDoesNotExposeItAsRowError()
            throws IOException {
        BulkUploadRow row = validRespondentRow();
        var internalFailure =
                new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR, "Unexpected validation failure");
        doThrow(internalFailure).when(validationSession).validate(any(), any());
        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);
        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception).isSameAs(internalFailure);
        assertThat(context.getValidationFailureMessages()).isEmpty();
        verify(persistenceService, times(0)).writeClob(any(), any());
    }

    @Test
    void givenPagePersistenceFailure_whenProcessing_thenReturnsSafeJobReferenceAndLogsDetails(
            CapturedOutput output) throws IOException {
        UUID jobId = UUID.randomUUID();
        BulkUploadRow firstRow = validOrganisationRow();
        BulkUploadRow secondRow = validOrganisationRow();
        JobContext context = new JobContext();
        var response =
                JobStatusResponse.builder()
                        .uuid(jobId)
                        .type(JobType.BULK_UPLOAD_ENTRIES)
                        .status(JobStatus.PROCESSING)
                        .userName("user")
                        .build();
        lifecycle.validating(
                new AsyncJobLifecycleEvent<>(
                        response, List.of(firstRow, secondRow), context, JobStatus.VALIDATING));
        AsyncJobLifecycleEvent<BulkUploadRow> event =
                new AsyncJobLifecycleEvent<>(
                        response, List.of(firstRow, secondRow), context, JobStatus.PROCESSING);
        var internalError =
                "ERROR: relation appreg.application_list does not exist [select * from secret]";
        when(bulkImportService.persistPage(any(), any()))
                .thenThrow(new IllegalStateException(internalError));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.processing(event));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_UPLOAD_PROCESSING_FAILED);
        var expectedMessage =
                "Bulk upload processing failed. Contact support quoting job reference "
                        + jobId
                        + ".";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
        assertThat(exception.getCause()).hasMessage(internalError);
        assertThat(context.getValidationFailureMessages())
                .containsExactly(expectedMessage)
                .allSatisfy(
                        message ->
                                assertThat(message)
                                        .doesNotContain(
                                                "ERROR", "relation", "appreg", "select", "secret"));
        assertThat(output)
                .contains("Failed to process bulk-import page")
                .contains("jobId=" + jobId)
                .contains(internalError);
        verify(bulkImportService).persistPage(any(), any());
    }

    @Test
    void givenRecognisedWordingValidationFailure_thenWritesRowErrorJsonAndCsv() throws IOException {
        when(csvFile.getBytes()).thenReturn("HEADER\nrow-two\n".getBytes());
        var writtenCsv = new StringBuilder();
        doAnswer(
                        invocation -> {
                            ByteArrayInputStream inputStream = invocation.getArgument(1);
                            writtenCsv.append(new String(inputStream.readAllBytes()));
                            return null;
                        })
                .when(persistenceService)
                .writeClob(any(), any());
        var row = validOrganisationRow();
        var applicationTexts = new ArrayListValuedHashMap<String, String>();
        applicationTexts.put("APPLICATION_TEXT1", "four characters");
        row.setApplicationTexts(applicationTexts);
        var context = new JobContext();
        var event = event(row, context);
        lifecycle.setCSVFile(csvFile);
        doAnswer(
                        invocation -> {
                            PayloadForCreate<EntryCreateDto> validatable =
                                    invocation.getArgument(0);
                            keyWordingFields(validatable);
                            BiFunction<
                                            PayloadForCreate<EntryCreateDto>,
                                            CreateApplicationEntryValidationSuccess,
                                            ?>
                                    callback = invocation.getArgument(1);
                            return callback.apply(
                                    validatable,
                                    validationResult("Application by {TEXT|Applicants name|4}"));
                        })
                .when(validationSession)
                .validate(any(), any());

        var exception = assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        assertThat(context.getValidationFailureMessages())
                .singleElement()
                .asString()
                .contains("\"rowNumber\":2")
                .contains("\"location\":\"APPLICATION_TEXT\"")
                .contains("Invalid length type in template: expected 4 but got 15");
        assertThat(writtenCsv.toString())
                .contains(
                        "row-two|APPLICATION_TEXT: Invalid length type in template: expected 4 but got 15");
        verify(persistenceService).writeClob(any(), any());
        verify(bulkImportService, times(0)).persistPage(any(), any());
    }

    @Test
    void givenAccountAndWordingValidationFailuresOnDifferentRows_thenWritesEachRowError()
            throws IOException {
        when(csvFile.getBytes()).thenReturn("HEADER\nrow-two\nrow-three\n".getBytes());
        var writtenCsv = new StringBuilder();
        doAnswer(
                        invocation -> {
                            ByteArrayInputStream inputStream = invocation.getArgument(1);
                            writtenCsv.append(new String(inputStream.readAllBytes()));
                            return null;
                        })
                .when(persistenceService)
                .writeClob(any(), any());
        var firstRow = validOrganisationRow();
        var secondRow = validOrganisationRow();
        var secondApplicationTexts = new ArrayListValuedHashMap<String, String>();
        secondApplicationTexts.put("APPLICATION_TEXT1", "four characters");
        secondRow.setApplicationTexts(secondApplicationTexts);
        var context = new JobContext();
        var event = event(List.of(firstRow, secondRow), context);
        lifecycle.setCSVFile(csvFile);
        var validationCount = new AtomicInteger();
        doAnswer(
                        invocation -> {
                            if (validationCount.getAndIncrement() == 0) {
                                throw new AppRegistryException(
                                        AppListEntryError
                                                .ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE,
                                        "Account number required for application code AP99001");
                            }
                            PayloadForCreate<EntryCreateDto> validatable =
                                    invocation.getArgument(0);
                            keyWordingFields(validatable);
                            BiFunction<
                                            PayloadForCreate<EntryCreateDto>,
                                            CreateApplicationEntryValidationSuccess,
                                            ?>
                                    callback = invocation.getArgument(1);
                            return callback.apply(
                                    validatable,
                                    validationResult("Application by {TEXT|Applicants name|4}"));
                        })
                .when(validationSession)
                .validate(any(), any());

        assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(context.getValidationFailureMessages())
                .singleElement()
                .asString()
                .contains("\"rowNumber\":2", "\"rowNumber\":3")
                .contains(
                        "Account number required for application code AP99001",
                        "Invalid length type in template: expected 4 but got 15");
        assertThat(writtenCsv.toString())
                .contains(
                        "row-two|ACCOUNT_NUMBER: Account number required for application code AP99001",
                        "row-three|APPLICATION_TEXT: Invalid length type in template: expected 4 but got 15");
        verify(persistenceService).writeClob(any(), any());
        verify(bulkImportService, times(0)).persistPage(any(), any());
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
                        .status(JobStatus.PROCESSING)
                        .userName("user")
                        .build();
        var duplicate = validOrganisationRow();
        var context = new JobContext();
        lifecycle.received(
                new AsyncJobLifecycleEvent<>(response, null, context, JobStatus.RECEIVED));
        lifecycle.validating(
                new AsyncJobLifecycleEvent<>(
                        response, List.of(duplicate, duplicate), context, JobStatus.VALIDATING));
        when(bulkImportService.persistPage(eq(jobId), any())).thenReturn(2);

        lifecycle.processing(
                new AsyncJobLifecycleEvent<>(
                        response, List.of(duplicate, duplicate), context, JobStatus.PROCESSING));
        lifecycle.completed(
                new AsyncJobLifecycleEvent<>(response, null, context, JobStatus.COMPLETED));

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
                        .status(JobStatus.PROCESSING)
                        .userName("user")
                        .build();
        var context = new JobContext();
        var rows = List.of(validOrganisationRow(), validOrganisationRow(), validOrganisationRow());
        lifecycle.validating(
                new AsyncJobLifecycleEvent<>(response, rows, context, JobStatus.VALIDATING));
        when(bulkImportService.persistPage(eq(jobId), any()))
                .thenAnswer(invocation -> invocation.<List<?>>getArgument(1).size());

        lifecycle.processing(
                new AsyncJobLifecycleEvent<>(
                        response, rows.subList(0, 2), context, JobStatus.PROCESSING));
        lifecycle.processing(
                new AsyncJobLifecycleEvent<>(
                        response, rows.subList(2, 3), context, JobStatus.PROCESSING));

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
        var event = event(row, context);
        var jobId = event.getResponse().getJobId().getId();

        try (MockedConstruction<ObjectMapper> mockedObjectMappers =
                mockConstruction(
                        ObjectMapper.class,
                        (mock, constructionContext) ->
                                when(mock.writeValueAsString(any()))
                                        .thenThrow(new JsonProcessingException("boom") {}))) {

            AppRegistryException exception =
                    assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));
            assertThat(mockedObjectMappers.constructed()).hasSize(1);

            assertThat(exception.getCode())
                    .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);
        }

        assertThat(context.getValidationFailureMessages())
                .containsExactly(
                        "Bulk upload processing failed. Contact support quoting job reference "
                                + jobId
                                + ".");

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

        lifecycle.failed(new AsyncJobLifecycleEvent<>(null, null, context, JobStatus.FAILED));

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
                .contains("row-two|")
                .contains("postcode - invalid: Provide a valid UK postcode.")
                .contains(row.getRespondentPostcode())
                .doesNotContain("Field has been rejected")
                .contains("row-three|");

        verify(persistenceService, times(1)).writeClob(any(), any());
    }

    @Test
    void givenMissingRespondentNames_whenValidating_thenJsonAndCsvContainSameFriendlyError()
            throws IOException {
        when(csvFile.getBytes()).thenReturn("HEADER\nrow-two\n".getBytes());

        StringBuilder writtenCsv = new StringBuilder();
        doAnswer(
                        invocation -> {
                            ByteArrayInputStream inputStream = invocation.getArgument(1);
                            writtenCsv.append(new String(inputStream.readAllBytes()));
                            return null;
                        })
                .when(persistenceService)
                .writeClob(any(), any());

        BulkUploadRow row = validRespondentRow();
        row.setRespondentTitle("");
        row.setRespondentFirstName(null);
        row.setRespondentLastName(null);
        JobContext context = new JobContext();
        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(row, context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        String errorDescription = context.getValidationFailureMessages().getFirst();
        assertThat(errorDescription)
                .contains("\"rowNumber\":2")
                .contains("\"location\":\"RESPONDENT\"")
                .contains("Respondent details are missing")
                .doesNotContain("must not be null")
                .doesNotContain("size must be between")
                .doesNotContain("respondent.person.name");
        assertThat(writtenCsv.toString())
                .contains(
                        "row-two|RESPONDENT: Respondent details are missing. Enter either"
                                + " Organisation Name, or Respondent First Name and Last Name.")
                .doesNotContain("must not be null")
                .doesNotContain("size must be between")
                .doesNotContain("respondent.person.name");

        verify(persistenceService).writeClob(any(), any());
    }

    @Test
    void whenValidating_csvFileIsSet_thenWritesClobSuccessfully_multipleRowErrorsForSingleRow()
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
        row.setRespondentEmail("testtest.com");

        JobContext context = new JobContext();
        AsyncJobLifecycleEvent<BulkUploadRow> event = event(row, context);

        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> lifecycle.validating(event));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(writtenCsv.toString())
                .contains("HEADER|")
                .contains("row-two|")
                .contains("email")
                .contains("testtest.com")
                .contains("Field has been rejected|")
                .contains("postcode")
                .contains(row.getRespondentPostcode())
                .contains("Provide a valid UK postcode.")
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
                .contains("row-two|postcode - invalid: Provide a valid UK postcode.")
                .contains("row-three|");

        verify(persistenceService, times(1)).writeClob(any(), any());
    }

    @Test
    void whenValidation_csvFileIsSet_containsHeaderError_thenWritesClobSuccesfully()
            throws IOException {
        when(csvFile.getBytes()).thenReturn("HEADER\nrow-two\n".getBytes());

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

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        assertThat(writtenCsv.toString())
                .contains("HEADER|")
                .contains(
                        "row-two|APPLICATION_TEXT: APPLICATION_TEXT1 is required for code AP99001")
                .doesNotContain("APPLICATION_TEXT - ");

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

        assertThat(exception.getMessage())
                .isEqualTo(
                        "Bulk upload processing failed. Contact support quoting job reference "
                                + event.getResponse().getJobId().getId()
                                + ".");
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
                .isEqualTo(
                        "Bulk upload processing failed. Contact support quoting job reference "
                                + event.getResponse().getJobId().getId()
                                + ".")
                .doesNotContain("write failed");
        assertThat(exception.getCause()).hasMessage("write failed");

        verify(persistenceService, times(1)).writeClob(any(), any());
    }

    private static AsyncJobLifecycleEvent<BulkUploadRow> event(
            BulkUploadRow row, JobContext context) {
        return event(List.of(row), context);
    }

    private static AsyncJobLifecycleEvent<BulkUploadRow> event(
            List<BulkUploadRow> rows, JobContext context) {
        return new AsyncJobLifecycleEvent<>(
                new JobStatusResponse(
                        UUID.randomUUID(),
                        JobType.BULK_UPLOAD_ENTRIES,
                        JobStatus.VALIDATING,
                        "user",
                        "error",
                        persistenceService),
                rows,
                context,
                JobStatus.VALIDATING);
    }

    private void assertFriendlyContactValidationFailure(
            BulkUploadRow row, String location, String rejectedValue, String message)
            throws IOException {
        JobContext context = new JobContext();
        lifecycle.setCSVFile(csvFile);

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> lifecycle.validating(event(row, context)));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_ROW_VALIDATION_FAILED);

        BulkUploadError[] errors =
                new ObjectMapper()
                        .readValue(
                                context.getValidationFailureMessages().getFirst(),
                                BulkUploadError[].class);
        assertThat(errors)
                .singleElement()
                .satisfies(
                        error -> {
                            assertThat(error.getLocation()).isEqualTo(location);
                            assertThat(error.getRejectedValue()).isEqualTo(rejectedValue);
                            assertThat(error.getMessage()).isEqualTo(message);
                        });
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

    private static CreateApplicationEntryValidationSuccess validationResult(String wording) {
        return CreateApplicationEntryValidationSuccess.builder()
                .wordingSentence(WordingTemplateSentence.with(wording))
                .build();
    }

    private static void keyWordingFields(PayloadForCreate<EntryCreateDto> validatable) {
        var wordingFields = validatable.getData().getWordingFields();
        validatable
                .getData()
                .setWordingFields(
                        List.of(
                                new TemplateSubstitution(
                                        "Applicants name", wordingFields.getFirst().getValue())));
    }
}
