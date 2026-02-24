package uk.gov.hmcts.appregister.controller.applicationentry;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;

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
        Assertions.assertFalse(entryGetDetailDto.getHasOffsiteFee());
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
}
