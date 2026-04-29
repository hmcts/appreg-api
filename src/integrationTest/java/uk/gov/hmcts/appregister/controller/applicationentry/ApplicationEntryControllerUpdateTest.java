package uk.gov.hmcts.appregister.controller.applicationentry;

import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationEntryControllerUpdateTest extends AbstractApplicationEntryCrudTest {
    @Autowired private DataAuditRepository dataAuditRepository;

    @Test
    public void givenASuccessfulUpdate_whenAllValueAreToBeUpdate_200Returned() throws Exception {
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setNumberOfRespondents(null);

        differenceLogAsserter.clearLogs();
        differenceLogAsserter.assertNoErrors();

        Response responseSpecCreate = createListEntryWithAllData();
        EntryGetDetailDto createdDetail = responseSpecCreate.as(EntryGetDetailDto.class);
        LocalDate createdDate = createdDetail.getLodgementDate();

        var tokenGenerator = createAdminToken();
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        EntryGetDetailDto updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);

        // make sure the update does not change the lodgement date and the
        // date it was created persists
        Assertions.assertEquals(createdDate, updatedDto.getLodgementDate());
        validateEntryUpdateResponse(
                entryUpdateDto,
                updatedDto,
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}",
                entryUpdateDto.getFeeStatuses());

        Response responseFindEntrySpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationEntryFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(
                                        updatedDto
                                                .getRespondent()
                                                .getPerson()
                                                .getName()
                                                .getSurname()),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        responseFindEntrySpec.then().statusCode(200);

        EntryPage page = responseFindEntrySpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(updatedDto.getId(), page.getContent().getFirst().getId());

        differenceLogAsserter.assertNoErrors();

        // (All your audit assertions can remain here unchanged)
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName()));
    }

    @Test
    public void givenASuccessfulUpdate_whenAllValueAreToBeUpdatedWithEnforcementFines_200Returned()
            throws Exception {
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setWordingFields(null);

        differenceLogAsserter.clearLogs();
        differenceLogAsserter.assertNoErrors();

        Response responseSpecCreate = createListEntryWithAllData();

        entryUpdateDto.setApplicationCode("EF1213");
        entryUpdateDto.setAccountNumber("1234567890");

        var tokenGenerator = createAdminToken();
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);

        EntryGetDetailDto updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);
        EntryGetDetailDto createDDto = responseSpecCreate.as(EntryGetDetailDto.class);

        validateEntryUpdateResponse(
                entryUpdateDto,
                updatedDto,
                "This is a test enforcement fine with no wording template substitution required",
                entryUpdateDto.getFeeStatuses());

        Response responseFindEntrySpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationEntryFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(
                                        updatedDto
                                                .getRespondent()
                                                .getPerson()
                                                .getName()
                                                .getSurname()),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        responseFindEntrySpec.then().statusCode(200);

        EntryPage page = responseFindEntrySpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(updatedDto.getId(), page.getContent().getFirst().getId());

        differenceLogAsserter.assertNoErrors();

        // (All your audit assertions can remain here unchanged)
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName()));
    }

    @Test
    public void
            givenOverlappingActiveApplicationCodesAndFees_whenUpdateListEntry_thenPreferNullEndDateRecords()
                    throws Exception {
        LocalDate today = LocalDate.now();
        String applicationCodeValue = "ZZ90002";
        String feeReference = "ZZ2.1";

        saveActiveApplicationCode(
                applicationCodeValue,
                feeReference,
                today.plusDays(30),
                "Fallback overlapping application code");
        final var preferredCode =
                saveActiveApplicationCode(
                        applicationCodeValue, feeReference, null, "Preferred application code");

        saveActiveFee(
                feeReference,
                "Fallback overlapping fee",
                BigDecimal.valueOf(222),
                false,
                today.plusDays(30));
        final var preferredFee =
                saveActiveFee(feeReference, "Preferred fee", BigDecimal.valueOf(111), false, null);

        Response responseSpecCreate = createListEntryWithAllData();
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setApplicationCode(applicationCodeValue);
        entryUpdateDto.setHasOffsiteFee(false);
        entryUpdateDto.setNumberOfRespondents(null);

        var tokenGenerator = createAdminToken();
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        EntryGetDetailDto updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);
        Assertions.assertEquals(
                preferredCode.getId(), getSelectedApplicationCodeId(updatedDto.getId()));
        Assertions.assertEquals(
                preferredFee.getId(), getSelectedFees(updatedDto.getId()).getFirst().getId());
    }

    @Test
    public void givenAFailureUpdate_whenAnEntryToUpdateDoesntExist_404Returned() throws Exception {
        var tokenGenerator = createAdminToken();
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(409);
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenAnEtagFailingMatch_412Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // 1) First update must succeed
        EntryUpdateDto firstUpdate = getCorrectUpdateDataDto();
        firstUpdate.setNumberOfRespondents(null);
        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(firstUpdate.getFeeStatuses());

        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        firstUpdate);

        responseSpecUpdate.then().statusCode(200);

        // 2) Second update with stale ETag must fail with 412
        EntryUpdateDto secondUpdate = getCorrectUpdateDataDto();
        secondUpdate.setNumberOfRespondents(null);
        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(secondUpdate.getFeeStatuses());

        Response responseSpecUpdateWithOldEtag =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        secondUpdate,
                        HeaderUtil.getETag(responseSpecCreate)); // stale etag from create

        responseSpecUpdateWithOldEtag.then().statusCode(412);
        ProblemAssertUtil.assertEquals(
                CommonAppError.MATCH_ETAG_FAILURE.getCode(), responseSpecUpdateWithOldEtag);
    }

    @Test
    public void givenInvalidUpdate_whenFeeStatusDueAndPaymentReferenceProvided_400Returned()
            throws Exception {

        FeeStatus feeStatus = new FeeStatus();
        feeStatus.setPaymentStatus(DUE);
        feeStatus.setPaymentReference("PAYREF-123");
        feeStatus.setStatusDate(LocalDate.now());

        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setFeeStatuses(List.of(feeStatus));

        Response responseSpecCreate = createListEntryWithAllData();

        var tokenGenerator = createAdminToken();

        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.PAYMENT_REFERENCE_NOT_ALLOWED_WHEN_PAYMENT_DUE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenWordingTemplateFieldsLengthNotAcceptable_400Returned()
            throws Exception {

        String stringExceedLength = RandomStringUtils.insecure().nextAlphanumeric(201);

        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Premises Address", stringExceedLength),
                        new TemplateSubstitution("Premises Date", LocalDate.now().toString())));

        var tokenGenerator = createAdminToken();
        Response entryResponse = createListEntryWithAllData();

        entryUpdateDto.setNumberOfRespondents(null);

        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        assert problemDetail.getDetail() != null;
        Assertions.assertEquals(
                "Premises Address=" + stringExceedLength, problemDetail.getDetail().trim());
    }

    @Test
    public void
            givenAnInvalidUpdateEntryRequest_whenEnforcementFineACAndNoAccountNumber_400IsReturned()
                    throws Exception {
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setWordingFields(null);

        differenceLogAsserter.clearLogs();
        differenceLogAsserter.assertNoErrors();

        Response responseSpecCreate = createListEntryWithAllData();

        // ensure we fail based on the account number
        entryUpdateDto.setApplicationCode("EF1213");
        entryUpdateDto.setAccountNumber(null);

        var tokenGenerator = createAdminToken();
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the error
        Assertions.assertEquals(400, responseSpecUpdate.statusCode());
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void givenASuccessUpdate_whenApplicantAddressIsValid_200Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("1 test road"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    /* REGEX Validation Tests */
    @Test
    public void givenASuccessUpdate_whenApplicantNameIsValid_200Returned() throws Exception {
        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.getRespondent().getPerson().getName().setSurname("test");
        entryUpdateDto.getRespondent().getPerson().getName().setFirstForename("François");
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("Joséphine"));
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("Sørina"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpecCreate = createListEntryWithAllData();

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    /* REGEX Validation Tests */
    @Test
    public void givenASuccessUpdate_whenApplicantEmailIsValid_200Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@test.com"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    /* REGEX Validation Tests */
    @Test
    public void givenASuccessUpdate_whenApplicantPhoneIsValid_200Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("01234 56789"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    /* REGEX Validation Tests */
    @Test
    public void givenASuccessUpdate_whenApplicantMobileIsValid_200Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("+447123456789"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    @Test
    public void givenAFailureUpdate_whenApplicantAddressInvalid_400Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("1\ttest\0road"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);

        responseSpecUpdate
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
    public void givenAFailureUpdate_whenApplicantInvalidName_400Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.getRespondent().getPerson().getName().setTitle("m\r");
        entryUpdateDto.getRespondent().getPerson().getName().setFirstForename("invalid\0name");
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("invalid\0 secondname"));
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("invalid\0 thirdname"));
        entryUpdateDto.getRespondent().getPerson().getName().setSurname("invalid\0surname");

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);

        responseSpecUpdate
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
    public void givenAFailureUpdate_whenApplicantPhoneNumberInvalid_400Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("\0 1234 56789"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);

        responseSpecUpdate
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
    public void givenAFailureUpdate_whenApplicantMobileInvalid_400Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("invalid-mobile"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);

        responseSpecUpdate
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
    public void givenAFailureUpdate_whenApplicantEmailInvalid_400Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("test-email@"));

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate
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
            givenACDoesNotRequireRespondent_andBulkRespondentAllowed_whenCreateEntryWithRespondent_thenReturn200()
                    throws Exception {

        // Arrange
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setFeeStatuses(null);
        entryUpdateDto.setStandardApplicantCode(null);

        // Use an app code which does NOT require a respondent but allows bulk respondent number
        entryUpdateDto.setApplicationCode("CT99001");
        TemplateSubstitution templateSubstitution = new TemplateSubstitution("Number", "5");
        entryUpdateDto.setWordingFields(List.of(templateSubstitution));

        var tokenGenerator = createAdminToken();

        Response responseSpecCreate = createListEntryWithAllData();

        // Act
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    @Test
    public void
            givenACDoesNotRequireRespondent_BulkRespondentAllowed_whenNumberOfRespondentsProvided_thenReturn200()
                    throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // Arrange
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setNumberOfRespondents(5);
        entryUpdateDto.setFeeStatuses(null);

        // Use an app code which does NOT require a respondent but allows bulk respondent number
        entryUpdateDto.setApplicationCode("CT99001");
        TemplateSubstitution templateSubstitution = new TemplateSubstitution("Number", "5");
        entryUpdateDto.setWordingFields(List.of(templateSubstitution));

        var tokenGenerator = createAdminToken();

        // Act
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);
    }

    @Test
    @DisplayName(
            "Update Application Entry persists write audit rows for DB-backed low-hanging fields")
    void givenBulkRespondentUpdate_whenUpdated_thenPersistWriteAuditRows() throws Exception {
        // Seed an entry with explicit starting values so the update assertions can check old and
        // new audit values directly from DATA_AUDIT.
        val entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setNumberOfRespondents(5);
        entryUpdateDto.setFeeStatuses(null);
        entryUpdateDto.setApplicationCode("CT99001");
        entryUpdateDto.setCaseReference("CASE-UPD-001");
        entryUpdateDto.setNotes("Updated audit notes");
        entryUpdateDto.setWordingFields(List.of(new TemplateSubstitution("Number", "5")));

        val tokenGenerator = createAdminToken();
        val responseSpecCreate =
                createListEntryWithAllData(
                        entryCreateDto -> entryCreateDto.setNotes("Original audit notes"));

        // Ignore the audit rows produced by the setup create request so we only inspect the update.
        dataAuditRepository.deleteAll();

        val responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        val updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);
        Assertions.assertEquals("Updated audit notes", updatedDto.getNotes());
        Assertions.assertEquals(5, updatedDto.getNumberOfRespondents());
        Assertions.assertEquals("CT99001", updatedDto.getApplicationCode());
        Assertions.assertEquals("CASE-UPD-001", updatedDto.getCaseReference());

        // Notes changed from the seeded value to the update payload value.
        val notesAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                                TableNames.APPLICATION_LISTS_ENTRY,
                                "notes",
                                "Original audit notes",
                                "Updated audit notes")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an application_list_entries.notes update audit row"));

        Assertions.assertEquals(
                AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName(),
                notesAuditRow.getEventName());

        // Bulk respondent count is new on update, so we check the new value and the update event.
        val bulkRespondentAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LISTS_ENTRY,
                                "number_of_bulk_respondents",
                                "5")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a number_of_bulk_respondents update audit row"));

        Assertions.assertEquals(
                AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName(),
                bulkRespondentAuditRow.getEventName());

        // Application code should record the nested DB-backed code change from the original create
        // code to the new bulk-respondent-capable code.
        val originalApplicationCode =
                responseSpecCreate.as(EntryGetDetailDto.class).getApplicationCode();
        val applicationCodeAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                                TableNames.APPLICATION_CODES,
                                "application_code",
                                originalApplicationCode,
                                "CT99001")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an application_codes.application_code update audit row"));

        Assertions.assertEquals(
                AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName(),
                applicationCodeAuditRow.getEventName());

        // Case reference is another DB-backed column on APPLICATION_LIST_ENTRIES and should record
        // the old and new values on update.
        val missingCaseReferenceAuditMessage =
                "Expected an application_list_entries.case_reference update audit row";
        val originalCaseReference =
                responseSpecCreate.as(EntryGetDetailDto.class).getCaseReference();
        val caseReferenceAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                                TableNames.APPLICATION_LISTS_ENTRY,
                                "case_reference",
                                originalCaseReference,
                                "CASE-UPD-001")
                        .orElseThrow(() -> new AssertionError(missingCaseReferenceAuditMessage));

        Assertions.assertEquals(
                AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName(),
                caseReferenceAuditRow.getEventName());
    }

    @Test
    @DisplayName(
            "Update Application Entry persists write audit rows for standard applicant selection")
    void givenStandardApplicantUpdate_whenUpdated_thenPersistStandardApplicantAuditRow()
            throws Exception {
        val entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setApplicant(null);
        entryUpdateDto.setStandardApplicantCode("APP002");
        entryUpdateDto.setNumberOfRespondents(null);

        val tokenGenerator = createAdminToken();
        val responseSpecCreate = createListEntryWithAllData();

        // Ignore the audit rows produced by the setup create request so we only inspect the update.
        dataAuditRepository.deleteAll();

        // Update the entry through the real endpoint so the standard-applicant reassignment goes
        // through the production validator, service and audit listener stack.
        val responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        val updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);
        Assertions.assertEquals("APP002", updatedDto.getStandardApplicantCode());

        // The update should record the selected standard applicant code in DATA_AUDIT.
        val missingAuditMessage =
                "Expected a standard_applicants.standard_applicant_code update audit row";
        val standardApplicantAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.STANDARD_APPLICANTS, "standard_applicant_code", "APP002")
                        .orElseThrow(() -> new AssertionError(missingAuditMessage));

        Assertions.assertEquals(
                AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName(),
                standardApplicantAuditRow.getEventName());
    }

    @Test
    @DisplayName("Update Application Entry persists child-row create and delete audit fields")
    void givenEntryWithChildRows_whenUpdated_thenPersistChildCreateAndDeleteAuditRows()
            throws Exception {
        val entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.getApplicant().setPerson(null);
        entryUpdateDto.getApplicant().getOrganisation().setName("Applicant Updated Org");
        entryUpdateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA12 1AA");
        entryUpdateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        entryUpdateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));
        entryUpdateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of(null));

        entryUpdateDto.getRespondent().setOrganisation(null);
        entryUpdateDto.getRespondent().getPerson().getName().setSurname("RespondentUpdated");

        val updatedOfficial = new Official();
        updatedOfficial.setTitle("Mrs");
        updatedOfficial.setForename("Uma");
        updatedOfficial.setSurname("OfficialUpdated");
        updatedOfficial.setType(OfficialType.MAGISTRATE);
        entryUpdateDto.setOfficials(List.of(updatedOfficial));

        val updatedFeeStatus = new FeeStatus();
        updatedFeeStatus.setPaymentReference("PAY-UPD-001");
        updatedFeeStatus.setPaymentStatus(PaymentStatus.REMITTED);
        updatedFeeStatus.setStatusDate(LocalDate.of(2026, 2, 1));
        entryUpdateDto.setFeeStatuses(List.of(updatedFeeStatus));

        val entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.getApplicant().setPerson(null);
        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto.getApplicant().getOrganisation().setName("Applicant Original Org");
        entryCreateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA12 1AA");
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
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of(null));
        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getName().setSurname("RespondentOriginal");

        val originalOfficial = new Official();
        originalOfficial.setTitle("Mr");
        originalOfficial.setForename("Oscar");
        originalOfficial.setSurname("OfficialOriginal");
        originalOfficial.setType(OfficialType.CLERK);
        entryCreateDto.setOfficials(List.of(originalOfficial));

        val originalFeeStatus = new FeeStatus();
        originalFeeStatus.setPaymentReference("PAY-OLD-001");
        originalFeeStatus.setPaymentStatus(PaymentStatus.PAID);
        originalFeeStatus.setStatusDate(LocalDate.of(2026, 1, 1));
        entryCreateDto.setFeeStatuses(List.of(originalFeeStatus));

        val tokenGenerator = createAdminToken();
        val responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(201);

        // Ignore the setup create rows so these assertions only inspect the update request.
        dataAuditRepository.deleteAll();

        // Drive the real update endpoint so all child replacements run through the production
        // delete/create audit operations.
        val responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        // Updating the applicant creates a replacement NAME_ADDRESS row with the new value.
        val createdApplicantAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.NAME_ADDRESS, "name", "Applicant Updated Org")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a created name_address.name applicant audit row"));
        Assertions.assertEquals(
                AppListEntryAuditOperation.CREATE_APPLICANT.getEventName(),
                createdApplicantAuditRow.getEventName());

        // The old applicant row should also be deleted and audited separately.
        val deletedApplicantAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.NAME_ADDRESS, "name", "Applicant Original Org")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a deleted name_address.name applicant audit row"));
        Assertions.assertEquals(
                AppListEntryAuditOperation.DELETE_APPLICANT.getEventName(),
                deletedApplicantAuditRow.getEventName());

        // Respondent replacement follows the same create/delete pattern on NAME_ADDRESS.
        val createdRespondentAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.NAME_ADDRESS, "surname", "RespondentUpdated")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a created name_address.surname respondent audit row"));
        Assertions.assertEquals(
                AppListEntryAuditOperation.CREATE_RESPONDENT.getEventName(),
                createdRespondentAuditRow.getEventName());

        val deletedRespondentAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.NAME_ADDRESS, "surname", "RespondentOriginal")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a deleted name_address.surname respondent audit row"));
        Assertions.assertEquals(
                AppListEntryAuditOperation.DELETE_RESPONDENT.getEventName(),
                deletedRespondentAuditRow.getEventName());

        // Officials are replaced on update, so we expect both delete and create rows.
        val createdOfficialAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLCATION_LISTS_ENTRY_OFFICIAL,
                                "surname",
                                "OfficialUpdated")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a created app_list_entry_official.surname audit row"));
        Assertions.assertEquals(
                AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY.getEventName(),
                createdOfficialAuditRow.getEventName());

        val deletedOfficialAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLCATION_LISTS_ENTRY_OFFICIAL,
                                "surname",
                                "OfficialOriginal")
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a deleted app_list_entry_official.surname audit row"));
        Assertions.assertEquals(
                AppListEntryAuditOperation.DELETE_OFFICIAL_ENTRY.getEventName(),
                deletedOfficialAuditRow.getEventName());

        // Fee statuses should now audit both the removed row and the replacement row on update.
        val missingCreatedFeeStatusAuditMessage =
                "Expected a created fee-status payment reference audit row";
        val createdFeeStatusAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LISTS_FEE_STATUS,
                                "alefs_payment_reference",
                                "PAY-UPD-001")
                        .orElseThrow(() -> new AssertionError(missingCreatedFeeStatusAuditMessage));
        Assertions.assertEquals(
                AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY.getEventName(),
                createdFeeStatusAuditRow.getEventName());

        val missingDeletedFeeStatusAuditMessage =
                "Expected a deleted fee-status payment reference audit row";
        val deletedFeeStatusAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LISTS_FEE_STATUS,
                                "alefs_payment_reference",
                                "PAY-OLD-001")
                        .orElseThrow(() -> new AssertionError(missingDeletedFeeStatusAuditMessage));
        Assertions.assertEquals(
                AppListEntryAuditOperation.DELETE_FEE_STATUS_ENTRY.getEventName(),
                deletedFeeStatusAuditRow.getEventName());
    }

    @Test
    public void
            givenACNotRequireRespondent_BulkRespondentAllowed_RespondentAndNumberOfRespondentsNotProvided_then400()
                    throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // Arrange
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setRespondent(null);
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setFeeStatuses(null);

        // Use an app code which does NOT require a respondent but allows bulk respondent number
        entryUpdateDto.setApplicationCode("CT99001");
        TemplateSubstitution templateSubstitution = new TemplateSubstitution("Number", "5");
        entryUpdateDto.setWordingFields(List.of(templateSubstitution));

        var tokenGenerator = createAdminToken();

        // Act
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecUpdate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                AppListEntryError.RESPONDENT_OR_NUMBER_OF_RESPONDENTS_REQUIRED
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    public void
            givenACNotRequireRespondent_BulkRespondentAllowed_RespondentAndNumberOfRespondentsProvided_then400()
                    throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // Arrange
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setStandardApplicantCode(null);
        entryUpdateDto.setNumberOfRespondents(10);
        entryUpdateDto.setFeeStatuses(null);

        // Use an app code which does NOT require a respondent but allows bulk respondent number
        entryUpdateDto.setApplicationCode("CT99001");
        TemplateSubstitution templateSubstitution = new TemplateSubstitution("Number", "5");
        entryUpdateDto.setWordingFields(List.of(templateSubstitution));

        var tokenGenerator = createAdminToken();

        // Act
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecUpdate
                .then()
                .statusCode(400)
                .body(
                        "type",
                        Matchers.equalTo(
                                AppListEntryError
                                        .BULK_RESPONDENT_NUMBER_AND_RESPONDENT_MUTUALLY_EXCLUSIVE
                                        .getCode()
                                        .getAppCode()));
    }

    @Test
    void givenTooManyMagistrates_whenUpdateEntry_thenReturn400() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setOfficials(
                List.of(
                        buildOfficial("Ms", "Maya", "One", OfficialType.MAGISTRATE),
                        buildOfficial("Mr", "Miles", "Two", OfficialType.MAGISTRATE),
                        buildOfficial("Mrs", "Mina", "Three", OfficialType.MAGISTRATE),
                        buildOfficial("Mr", "Marco", "Four", OfficialType.MAGISTRATE)));

        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        createAdminToken().fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(400);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.TOO_MANY_MAGISTRATES.getCode(), responseSpecUpdate);
    }

    @Test
    void givenTooManyCourtOfficials_whenUpdateEntry_thenReturn400() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setOfficials(
                List.of(
                        buildOfficial("Ms", "Maya", "One", OfficialType.MAGISTRATE),
                        buildOfficial("Mr", "Chris", "CourtOne", OfficialType.CLERK),
                        buildOfficial("Mrs", "Clare", "CourtTwo", OfficialType.CLERK)));

        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        createAdminToken().fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(400);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.TOO_MANY_COURT_OFFICIALS.getCode(), responseSpecUpdate);
    }

    @Test
    void givenNoOfficials_whenUpdateEntry_thenReturn200() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setOfficials(List.of());

        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        createAdminToken().fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        EntryGetDetailDto updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);
        validateEntryUpdateResponse(
                entryUpdateDto,
                updatedDto,
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}",
                entryUpdateDto.getFeeStatuses());
        Assertions.assertTrue(updatedDto.getOfficials().isEmpty());
    }

    private static Official buildOfficial(
            String title, String forename, String surname, OfficialType type) {
        Official official = new Official();
        official.setTitle(title);
        official.setForename(forename);
        official.setSurname(surname);
        official.setType(type);
        return official;
    }
}
