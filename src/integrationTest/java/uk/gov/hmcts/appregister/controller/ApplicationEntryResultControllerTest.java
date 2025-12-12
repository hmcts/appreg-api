package uk.gov.hmcts.appregister.controller;

import com.nimbusds.jose.JOSEException;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;

public class ApplicationEntryResultControllerTest extends AbstractSecurityControllerTest {

    @MockitoBean
    private UserProvider provider;

    private static final String WEB_CONTEXT = "application-lists";

    @BeforeEach
    public void before() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 204 when valid IDs")
    void givenValidIds_whenDelete_then204() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createAndSaveEntry(list);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();

        // fire test
        Response resp = deleteResult(list.getUuid(), entry.getUuid(), entryResult.getUuid(), token);

        // assert success
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 404 when list unknown")
    void givenUnknownList_whenDelete_then404() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        assertProblemDetailType(resp, ApplicationListError.ENTRY_RESULT_LIST_NOT_FOUND.getCode().getAppCode());
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when list closed")
    void givenClosedList_whenDelete_then400() throws Exception {
        var list = createAndSaveList(CLOSED);

        UUID listId = list.getUuid();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertProblemDetailType(resp, ApplicationListError.INVALID_ENTRY_RESULT_LIST_STATUS.getCode().getAppCode());
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry not in list")
    void givenEntryNotInList_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createAndSaveEntry(list);
        persistance.save(entry);

        UUID listId = list.getUuid();
        UUID entryId = entry.getUuid();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertProblemDetailType(resp, ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode().getAppCode());
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry result not related to entry")
    void givenEntryResultNotRelatedToEntry_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createAndSaveEntry(list);
        persistance.save(entry);
        var unrelatedEntry = createAndSaveEntry(list);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(unrelatedEntry, resolutionCode);

        UUID listId = list.getUuid();
        UUID entryId = entry.getUuid();
        UUID resultId = entryResult.getUuid();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertProblemDetailType(resp, ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode().getAppCode());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
            RestEndpointDescription.builder()
                .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()
                                     + "/entries/" + UUID.randomUUID()
                                     + "/results/" + UUID.randomUUID()))
                .method(HttpMethod.DELETE)
                .successRole(RoleEnum.USER)
                .successRole(RoleEnum.ADMIN)
                .build()
        );
    }

    private TokenAndJwksKey getToken() throws JOSEException {
        return getATokenWithValidCredentials()
            .roles(List.of(RoleEnum.USER))
            .build()
            .fetchTokenForRole();
    }

    private Response deleteResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token)
        throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
            getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results/" + resultId),
            token);
    }

    private ApplicationList createAndSaveList(Status status) {
        var list = new AppListTestData()
            .someMinimal()
            .status(status)
            .build();
        persistance.save(list);
        return list;
    }

    private ApplicationListEntry createAndSaveEntry(ApplicationList list) {
        return new AppListEntryTestData()
            .someMinimal()
            .applicationList(list)
            .build();
    }

    private AppListEntryResolution createAndSaveResolution(ApplicationListEntry entry,
                                                           ResolutionCode resolutionCode) {
        var resolution = new AppListEntryResolutionTestData()
            .someMinimal()
            .resolutionWording(AppListEntryResolutionTestData.WORDING_1)
            .applicationList(entry)
            .resolutionCode(resolutionCode)
            .build();
        return persistance.save(resolution);
    }

    private void assertProblemDetailType(Response resp, String expectedAppCode) {
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(expectedAppCode, problemDetail.getType().toString());
    }
}
