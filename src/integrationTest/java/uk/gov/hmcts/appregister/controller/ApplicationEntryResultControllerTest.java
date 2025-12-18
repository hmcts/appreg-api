package uk.gov.hmcts.appregister.controller;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.TableNames;
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
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;

public class ApplicationEntryResultControllerTest extends AbstractSecurityControllerTest {

    @MockitoBean private UserProvider provider;

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
        var entry = createEntry(list);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();

        // fire test
        Response resp = deleteResult(list.getUuid(), entry.getUuid(), entryResult.getUuid(), token);

        // assert success
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "version",
                        null,
                        null,
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "aler_id",
                        null,
                        null,
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getEventName()));
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 404 when list unknown")
    void givenUnknownList_whenDelete_then404() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.ENTRY_RESULT_LIST_NOT_FOUND.getCode(), resp);
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
        assertEquals(
                ApplicationListEntryResultError.INVALID_ENTRY_RESULT_LIST_STATUS.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry not in list")
    void givenEntryNotInList_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        UUID listId = list.getUuid();
        UUID entryId = entry.getUuid();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry result not related to entry")
    void givenUnrelatedEntry_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var unrelatedEntry = createEntry(list);
        persistance.save(unrelatedEntry);
        var entry = createEntry(list);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        UUID listId = list.getUuid();
        UUID entryId = unrelatedEntry.getUuid();
        UUID resultId = entryResult.getUuid();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode(), resp);
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        WEB_CONTEXT
                                                + "/"
                                                + UUID.randomUUID()
                                                + "/entries/"
                                                + UUID.randomUUID()
                                                + "/results/"
                                                + UUID.randomUUID()))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private Response deleteResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token)
            throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
                getLocalUrl(
                        WEB_CONTEXT
                                + "/"
                                + listId
                                + "/entries/"
                                + entryId
                                + "/results/"
                                + resultId),
                token);
    }

    private ApplicationList createAndSaveList(Status status) {
        var list = new AppListTestData().someMinimal().status(status).build();
        persistance.save(list);
        return list;
    }

    private ApplicationListEntry createEntry(ApplicationList list) {
        return new AppListEntryTestData().someMinimal().applicationList(list).build();
    }

    private AppListEntryResolution createAndSaveResolution(
            ApplicationListEntry entry, ResolutionCode resolutionCode) {
        var resolution =
                new AppListEntryResolutionTestData()
                        .someMinimal()
                        .resolutionWording(AppListEntryResolutionTestData.WORDING_1)
                        .applicationList(entry)
                        .resolutionCode(resolutionCode)
                        .build();
        return persistance.save(resolution);
    }
}
