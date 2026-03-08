package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ApplicationEntryControllerReadTest extends AbstractApplicationEntryCrudTest {

    @Test
    @StabilityTest
    public void testGetApplicationEntrySuccess() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + uuids[0] + "/entries/" + uuids[1]),
                        tokenGenerator.fetchTokenForRole());

        EntryGetDetailDto entryGetDetailDto = responseSpec.as(EntryGetDetailDto.class);
        Assertions.assertEquals(200, responseSpec.getStatusCode());
        Assertions.assertEquals("APP002", entryGetDetailDto.getStandardApplicantCode());
        Assertions.assertEquals("AD99002", entryGetDetailDto.getApplicationCode());
        Assertions.assertEquals("Rescheduled due to missing docs", entryGetDetailDto.getNotes());
        Assertions.assertEquals("CASE123457", entryGetDetailDto.getCaseReference());
        assertFalse(entryGetDetailDto.getHasOffsiteFee());
        Assertions.assertEquals(uuids[1], entryGetDetailDto.getId());
        Assertions.assertEquals(uuids[0], entryGetDetailDto.getListId());
    }

    @Test
    public void testGetApplicationEntryListDoesNotExist() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries/"
                                        + uuids[1]),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListIsClosedExist() throws Exception {
        var tokenGenerator = createAdminToken();

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getClosedApplicationListId()
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListWithIsDeleted() throws Exception {
        var tokenGenerator = createAdminToken();

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getDeletedIdApplicationListId()
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListWithEntryNotPartOfList() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);
        UUID[] uuids2 = getValidEntryForList(VALID_ENTRY2_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT + "/" + uuids[0] + "/entries/" + uuids2[1]),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListWithEntryNotInList() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + uuids[0]
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntriesListSuccess() throws Exception {
        EntryApplicationListGetFilterDto filterDto = new EntryApplicationListGetFilterDto();
        filterDto.setRespondentOrganisation("Sarah Johnson");

        UUID applicationListId = getOpenApplicationListId();
        UnaryOperator<RequestSpecification> filterOperator =
                new ApplicationEntryFilterByApplicationId(
                        applicationListId,
                        Optional.ofNullable(filterDto.getApplicantOrganisation()),
                        Optional.ofNullable(filterDto.getApplicantSurname()),
                        Optional.ofNullable(filterDto.getRespondentOrganisation()),
                        Optional.ofNullable(filterDto.getRespondentSurname()),
                        Optional.ofNullable(filterDto.getRespondentPostcode()),
                        Optional.ofNullable(filterDto.getAccountReference()),
                        Optional.ofNullable(filterDto.getApplicationTitle()),
                        Optional.ofNullable(filterDto.getFeeRequired()),
                        Optional.ofNullable(filterDto.getSequenceNumber()));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(20),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationListId.toString()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        filterOperator,
                        new OpenApiPageMetaData());

        Assertions.assertEquals(200, responseSpec.getStatusCode());

        EntryPage page = responseSpec.as(EntryPage.class);
        assertEquals(1, (int) page.getTotalPages());
        assertEquals(1, page.getTotalElements());
        assertFalse(page.getContent().isEmpty());

        var firstEntry = page.getContent().getFirst();
        assertEquals(applicationListId, firstEntry.getListId());
        assertEquals("Copy documents (electronic)", firstEntry.getApplicationTitle());
        assertEquals(LocalDate.parse("2024-04-21"), firstEntry.getDate());
        assertEquals(ApplicationListStatus.OPEN, firstEntry.getStatus());
        assertEquals(true, firstEntry.getIsFeeRequired());
    }

    @Test
    public void testGetApplicationEntriesListSuccess_emptyContent() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build();

        // creating application list to ensure it has no entries
        UUID applicationListId =
                createApplicationList(
                        tokenGenerator.fetchTokenForRole(), uniquePrefix("empty_app_list_entries"));

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationListId.toString()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole());
        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(200, responseSpec.getStatusCode());
        assertEquals(0, (int) page.getTotalPages());
        Assertions.assertTrue(page.getContent().isEmpty());
    }

    @Test
    public void testGetApplicationEntriesListFailure_applicationListNotFound() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build();

        // creating application list to ensure it has no entries
        UUID applicationListId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + applicationListId + "/entries"),
                        tokenGenerator.fetchTokenForRole());
        Assertions.assertEquals(404, responseSpec.getStatusCode());

        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getType().get(),
                problemDetail.getType());
    }

    private UUID createApplicationList(TokenAndJwksKey token, String prefix) throws Exception {
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(prefix + " - list")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        Response createListResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl("application-lists"), token, createListReq);
        createListResp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto createdList =
                createListResp.as(ApplicationListGetDetailDto.class);
        return createdList.getId();
    }

    private static String uniquePrefix(String base) {
        return base + " :: " + UUID.randomUUID();
    }
}
