package uk.gov.hmcts.appregister.controller.applicationentry;

import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationEntryControllerCreateTest extends AbstractApplicationEntryCrudTest {

    @Test
    public void givenValidRequest_whenCreateListEntry_thenReturn201() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        var tokenGenerator = createAdminToken();

        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto(),
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}");

        EntryPage page = findEntriesBySurname(tokenGenerator, surnameToLookup, 10, 0);

        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(
                createdDto.getDetailDto().getId(), page.getContent().getFirst().getId());

        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "id",
                        "",
                        null,
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getType()
                                .name(),
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getType()
                                .name(),
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        "",
                        null,
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getType()
                                .name(),
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getEventName()));
    }

    @Test
    public void givenValidRequest_whenCreateListEntryWithEnforcementFineCode_thenReturn201()
            throws Exception {
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(null);

        var tokenGenerator = createAdminToken();

        // set the enforcement fine code
        entryCreateDto.setApplicationCode("EF1213");
        entryCreateDto.setAccountNumber("1234567890");

        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto(),
                "This is a test enforcement fine with no wording template substitution required");

        EntryPage page = findEntriesBySurname(tokenGenerator, surnameToLookup, 10, 0);

        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(
                createdDto.getDetailDto().getId(), page.getContent().getFirst().getId());

        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "id",
                        "",
                        null,
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getType()
                                .name(),
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getType()
                                .name(),
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        "",
                        null,
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getType()
                                .name(),
                        uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation
                                .CREATE_APP_ENTRY_LIST
                                .getEventName()));
    }

    @Test
    public void givenValidRequest_whenCreateListEntryWithoutFeeAndRespondent_thenReturn201()
            throws Exception {

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(List.of());
        entryCreateDto.setApplicationCode("AD99004");
        entryCreateDto.setRespondent(null);
        entryCreateDto.setFeeStatuses(List.of());

        var tokenGenerator = createAdminToken();

        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(
                        tokenGenerator, entryCreateDto, UUID.randomUUID().toString());

        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto(),
                "Request for a certificate of satisfaction of "
                        + "debt registered in the register of judgements, orders and fines");
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenCreateEntryWithApplicantApplicantMutualExclusiveInvalid_400IsReturned()
                    throws Exception {

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        List<Official> officials = Instancio.ofList(Official.class).size(4).create();
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(officials);

        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));

        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));
        entryCreateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));

        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("RESPONDENT@TEST.COM"));

        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode("APP001");

        String surnameToLookup = Instancio.gen().string().get();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);
        entryCreateDto.getApplicant().getPerson().getName().setThirdForename(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));

        entryCreateDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Premises Address", "test wording"),
                        new TemplateSubstitution("Premises Date", LocalDate.now().toString())));

        var tokenGenerator = createAdminToken();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenApplicationListDoesNotexist_404IsReturned()
            throws Exception {

        var tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + UUID.randomUUID() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(409);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenApplicationListIsNotInCorrectState_400IsReturned()
                    throws Exception {

        var tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getClosedApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(409);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenApplicationCodeDoesNotExist_404IsReturned()
            throws Exception {

        var tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicationCode("INVALID");

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(404);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenFeeNotExist_404IsReturned() throws Exception {
        var tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.getFeeStatuses().clear();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.FEE_REQUIRED.getCode().getType().get(), problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenRespondentNotExist_404IsReturned()
            throws Exception {

        var tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenWordingTemplateFieldsNotSufficient_400IsReturned()
                    throws Exception {

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Premises Address", "value"),
                        new TemplateSubstitution("Premises Date", "extra field not a date"),
                        new TemplateSubstitution("too many", "val")));

        var tokenGenerator = createAdminToken();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenFeeStatusIsDueAndPaymentReferenceProvided_then400IsReturned()
                    throws Exception {

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        Assertions.assertNotNull(entryCreateDto.getFeeStatuses());
        Assertions.assertFalse(entryCreateDto.getFeeStatuses().isEmpty());

        FeeStatus feeStatus = entryCreateDto.getFeeStatuses().getFirst();
        feeStatus.setPaymentStatus(DUE);
        feeStatus.setPaymentReference("PAYREF-123");

        var tokenGenerator = createAdminToken();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);

        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.PAYMENT_REFERENCE_NOT_ALLOWED_WHEN_PAYMENT_DUE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @StabilityTest
    public void givenCreatedEntrySoftDeletedViaRepository_whenSearchingEntries_thenEntryIsExcluded()
            throws Exception {

        var tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String uniqueSurname = "DELTEST-" + UUID.randomUUID();

        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, uniqueSurname);

        UUID createdUuid = createdDto.getDetailDto().getId();

        int rowsUpdated =
                unitOfWork.inTransaction(
                        () -> applicationListEntryRepository.softDeleteByUuid(createdUuid));
        Assertions.assertEquals(1, rowsUpdated);

        int pageSize = Math.max(defaultPageSize, 100);
        var page = findAllEntriesWithLargePage(tokenGenerator, pageSize, 0);

        boolean foundDeleted =
                page.getContent() != null
                        && page.getContent().stream()
                                .anyMatch(s -> s.getId() != null && s.getId().equals(createdUuid));

        Assertions.assertFalse(foundDeleted);
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenEnforcementFineACAndNoAccountNumber_409IsReturned()
                    throws Exception {
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(List.of());

        var tokenGenerator = createAdminToken();

        // set the enforcement fine code for a failure
        entryCreateDto.setApplicationCode("EF1213");
        entryCreateDto.setAccountNumber(null);

        // run the test
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the error
        Assertions.assertEquals(409, responseSpecCreate.statusCode());
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_NUMBER_REQUIRED_FOR_APPLICATION_CODE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenCreateEntryWithAppicantApplicantMutualExclusiveInvalid2_400IsReturned()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));

        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));
        entryCreateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenCreateEntryWithRespondentMutualExclusiveInvalid_400IsReturned()
                    throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));
        entryCreateDto.getApplicant().setOrganisation(null);

        entryCreateDto.getRespondent().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getRespondent()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("APPLICANT@TEST.COM"));
        entryCreateDto.getRespondent().getOrganisation().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("RESPONDENT@TEST.COM"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));

        entryCreateDto.setNumberOfRespondents(10);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);
        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenApplicationListIsNotInCorrectStateDeleted_400IsReturned()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getDeletedIdApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(409);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenRespondentNotRequired_400IsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenWordingLengthNotSufficient_400IsReturned()
            throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("only one field that exceeds length");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field");

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Address=only one field that exceeds length",
                problemDetail.getDetail().trim());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenWordingDataTypeFailure_400IsReturned()
            throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field not a date");

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Date=extra field not a date", problemDetail.getDetail().trim());
    }

    @Test
    public void givenASuccessCreate_whenEntryCreateDTOApplicantHasValidName_201Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.getApplicant().getPerson().getName().setFirstForename("vålid");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("valid"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("vœlid"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
    }

    @Test
    public void givenASuccessCreate_whenEntryCreateDTOApplicantHasValidAddress_201Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine1("1 valid address");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("2 valid address"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of("3 valid address"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of("4 valid address"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of("5 valid address"));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
    }

    @Test
    public void givenASuccessCreate_whenEntryCreateDTOApplicantHasValidPhoneNumber_201Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("01234 56789"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
    }

    @Test
    public void givenASuccessCreate_whenEntryCreateDTOApplicantHasValidMobileNumber_201Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("+447123456890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
    }

    @Test
    public void givenASuccessCreate_whenEntryCreateDTOApplicantHasValidEmail_201Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@valid.com"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTORespondentHasInvalidName_400Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicant(null);
        entryCreateDto.getRespondent().getPerson().getName().setFirstForename("invalid\0");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("inv\nalid"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("\t lastname"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTORespondentHasInvalidAddress_400Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicant(null);
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("\r cambridge"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(Instancio.gen().string().get()));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTORespondentHasInvalidPostcode_400Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicant(null);
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("invalid");

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTORespondentHasInvalidPhoneNumber_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicant(null);
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("012234\n567890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTORespondentHasInvalidMobileNumber_400Returned()
            throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicant(null);
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("072234\n567890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTORespondentHasInvalidEmailAddress_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setApplicant(null);
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("invalid@email"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOApplicantHasInvalidName_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().getPerson().getName().setFirstForename("invalid\0");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("inv\nalid"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("\t lastname"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOApplicantHasInvalidAddress_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("\r cambridge"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(Instancio.gen().string().get()));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOApplicantHasInvalidPostcode_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("invalid");

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOApplicantHasInvalidPhoneNumber_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("012234\n567890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOApplicantHasInvalidMobileNumber_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("072234\n567890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOApplicantHasInvalidEmailAddress_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("invalid@email"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOOrganisationHasInvalidName_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto.getApplicant().getOrganisation().setName("Big\tMoney");

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOOrganisationHasInvalidAddress_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("\r cambridge"));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(Instancio.gen().string().get()));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenAFailureCreate_whenEntryCreateDTOOrganisationHasInvalidPostcode_400Returned()
            throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("invalid");

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void
            givenAFailureCreate_whenEntryCreateDTOOrganisationHasInvalidPhoneNumber_400Returned()
                    throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of("012234\n567890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void
            givenAFailureCreate_whenEntryCreateDTOOrganisationHasInvalidMobileNumber_400Returned()
                    throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of("072234\n567890"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void
            givenAFailureCreate_whenEntryCreateDTOOrganisationHasInvalidEmailAddress_400Returned()
                    throws Exception {

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("invalid@email"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void givenSameList_whenCreateTwoEntries_thenSequenceNumbersIncrement() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        // Arrange: create two entries in the same open list
        EntryCreateDto dto1 = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        dto1.getApplicant().getPerson().getName().setSurname("SEQ-" + UUID.randomUUID());

        EntryCreateDto dto2 = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        dto2.getApplicant().getPerson().getName().setSurname("SEQ-" + UUID.randomUUID());

        SuccessCreateEntryResponse created1 =
                createEntryWithUniqueSurname(
                        tokenGenerator,
                        dto1,
                        dto1.getApplicant().getPerson().getName().getSurname());
        SuccessCreateEntryResponse created2 =
                createEntryWithUniqueSurname(
                        tokenGenerator,
                        dto2,
                        dto2.getApplicant().getPerson().getName().getSurname());

        UUID entryUuid1 = created1.getDetailDto().getId();
        UUID entryUuid2 = created2.getDetailDto().getId();

        // Assert: sequence_number is increasing
        Short seq1 = getSequenceNumberFromDb(entryUuid1);
        Short seq2 = getSequenceNumberFromDb(entryUuid2);

        Assertions.assertNotNull(seq1);
        Assertions.assertNotNull(seq2);

        Assertions.assertEquals(
                (short) (seq1 + 1),
                seq2,
                "Expected second sequence to be exactly seq1 + 1: " + seq1 + " -> " + seq2);
    }

    @Test
    public void givenDifferentLists_whenCreateEntries_thenSequencesAreIndependent()
            throws Exception {
        List<ApplicationList> lists =
                unitOfWork.inTransaction(
                        () -> applicationListRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));

        Assertions.assertTrue(
                lists.size() >= 2, "Need at least two application lists for this test");

        ApplicationList list1 = lists.get(0);
        ApplicationList list2 = lists.get(1);

        Assertions.assertNotEquals(list1.getUuid(), list2.getUuid(), "Lists must be different");

        // Create entry in list 1
        EntryCreateDto dto1 = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        dto1.getApplicant().getPerson().getName().setSurname("SEQ-L1-" + UUID.randomUUID());
        TokenGenerator tokenGenerator = createAdminToken();
        Response r1 =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list1.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        dto1);
        r1.then().statusCode(201);
        EntryGetDetailDto created1 = r1.as(EntryGetDetailDto.class);

        // Create entry in list 2
        EntryCreateDto dto2 = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        dto2.getApplicant().getPerson().getName().setSurname("SEQ-L2-" + UUID.randomUUID());
        Response r2 =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list2.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        dto2);
        r2.then().statusCode(201);
        EntryGetDetailDto created2 = r2.as(EntryGetDetailDto.class);

        Short seq1 = getSequenceNumberFromDb(created1.getId());
        Short seq2 = getSequenceNumberFromDb(created2.getId());

        Assertions.assertNotNull(seq1);
        Assertions.assertNotNull(seq2);
    }

    @Test
    public void givenNewList_whenCreateFirstEntry_thenSequenceIsOne() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey tokenAndJwks = tokenGenerator.fetchTokenForRole();

        var createListReq =
                new ApplicationListCreateDto()
                        .date(LocalDate.now().plusDays(1))
                        .time(LocalTime.of(10, 0))
                        .description("SEQ TEST LIST " + UUID.randomUUID())
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode("CCC003")
                        .durationHours(1)
                        .durationMinutes(0);

        Response createListResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl("application-lists"), tokenAndJwks, createListReq);

        createListResp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto createdList =
                createListResp.as(ApplicationListGetDetailDto.class);

        UUID listUuid = createdList.getId();

        EntryCreateDto entryReq = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryReq.getApplicant().getPerson().getName().setSurname("SEQ-FIRST-" + UUID.randomUUID());

        Response createEntryResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl("application-lists/" + listUuid + "/entries"),
                        tokenAndJwks,
                        entryReq);

        createEntryResp.then().statusCode(HttpStatus.CREATED.value());

        EntryGetDetailDto createdEntry = createEntryResp.as(EntryGetDetailDto.class);

        Short seq = getSequenceNumberFromDb(createdEntry.getId());

        Assertions.assertNotNull(seq, "Created entry must have a sequence number");
        Assertions.assertEquals(
                Short.valueOf((short) 1),
                seq,
                "First allocated sequence should be 1 for a new list");
    }

    private Short getSequenceNumberFromDb(UUID entryUuid) {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationListEntry entry =
                            applicationListEntryRepository
                                    .findByUuid(entryUuid)
                                    .orElseThrow(
                                            () ->
                                                    new AssertionError(
                                                            "Entry not found: " + entryUuid));
                    return entry.getSequenceNumber();
                });
    }

    @Test
    public void
            givenApplicationCodeDoesNotRequireRespondent_whenCreateEntryWithRespondentProvided_thenReturn201()
                    throws Exception {
        // Arrange
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        // Use an app code which does NOT require a respondent
        entryCreateDto.setApplicationCode("AD99004");

        Assertions.assertNotNull(
                entryCreateDto.getRespondent(),
                "Test requires respondent to be present in payload");

        entryCreateDto.setWordingFields(List.of());
        entryCreateDto.setFeeStatuses(List.of());

        var tokenGenerator = createAdminToken();
        String surnameToLookup = UUID.randomUUID().toString();

        // Act
        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        // Assert
        Assertions.assertNotNull(createdDto);
        Assertions.assertNotNull(createdDto.getDetailDto());
        Assertions.assertNotNull(createdDto.getDetailDto().getId());
        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));
    }
}
