package uk.gov.hmcts.appregister.controller.applicationentry;

import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationEntryControllerUpdateTest extends AbstractApplicationEntryCrudTest {

    @Test
    public void givenASuccessfulUpdate_whenAllValueAreToBeUpdate_200Returned() throws Exception {
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setNumberOfRespondents(null);

        differenceLogAsserter.clearLogs();
        differenceLogAsserter.assertNoErrors();

        Response responseSpecCreate = createListEntryWithAllData();

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
                List.of("Premises Address", "Premises Date"),
                createDDto.getFeeStatuses());

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
                entryUpdateDto, updatedDto, List.of(), createDDto.getFeeStatuses());

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
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName()));
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
            givenAnInvalidUpdateEntryRequest_whenEnforcementFineACAndNoAccountNumber_409IsReturned()
                    throws Exception {
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setWordingFields(null);

        differenceLogAsserter.clearLogs();
        differenceLogAsserter.assertNoErrors();

        Response responseSpecCreate = createListEntryWithAllData();

        // ensure we fail based on the account number
        entryUpdateDto.setApplicationCode("EF1213");
        entryUpdateDto.setAccountNumber("");

        var tokenGenerator = createAdminToken();
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the error
        Assertions.assertEquals(409, responseSpecUpdate.statusCode());
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_NUMBER_REQUIRED_FOR_APPLICATION_CODE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }
}
