package uk.gov.hmcts.appregister.controller;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

public class ActionControllerTest extends AbstractSecurityControllerTest {
    @Autowired private EntityManager entityManager;
    @MockitoBean private UserProvider provider;
    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    private static final String UNKNOWN_APPLICATION_LIST_ID =
            "ffffffff-ffff-ffff-ffff-ffffffffffff";

    @BeforeEach
    public void before() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

    @Test
    @DisplayName("Move Application List Entries")
    void givenValidRequest_whenMove_then200() throws Exception {
        // Given: source, target and other lists
        ApplicationList sourceList = new AppListTestData().someMinimal().build();
        ApplicationListEntry sourceEntry = new AppListEntryTestData().someMinimal().build();
        sourceEntry.setApplicationList(sourceList);

        persistance.save(sourceEntry);
        sourceList.setStatus(Status.OPEN);
        persistance.save(sourceList);

        ApplicationList targetList = new AppListTestData().someMinimal().build();
        ApplicationListEntry targetListEntry = new AppListEntryTestData().someMinimal().build();
        targetListEntry.setApplicationList(targetList);

        persistance.save(targetListEntry);
        targetList.setStatus(Status.OPEN);
        persistance.save(targetList);

        Set<UUID> uuidsToMove = Set.of(sourceEntry.getUuid());

        var token = getToken();

        // fire test
        Response resp =
                getMoveApplicationListEntriesResponse(
                        sourceList.getUuid(), targetList.getUuid(), uuidsToMove, token);

        // assert success
        resp.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Move Application List Entries: 404 when source list unknown")
    void givenUnknownTargetApplicationList_whenMoveApplicationListEntries_then404()
            throws Exception {
        var token = getToken();

        ApplicationListPage page = getApplicationListPage(token, ApplicationListStatus.OPEN);

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());

        Response resp =
                getMoveApplicationListEntriesResponse(
                        UUID.fromString(UNKNOWN_APPLICATION_LIST_ID),
                        page.getContent().getFirst().getId(),
                        entryIds,
                        token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.SOURCE_LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Move Application List Entries: 404 when target list unknown")
    void givenUnknownSourceApplicationList_whenMoveApplicationListEntries_then404()
            throws Exception {
        var token = getToken();

        ApplicationListPage page = getApplicationListPage(token, ApplicationListStatus.OPEN);

        UUID sourceListId = page.getContent().getFirst().getId();

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());

        // fire test
        Response resp =
                getMoveApplicationListEntriesResponse(
                        sourceListId, UUID.randomUUID(), entryIds, token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.TARGET_LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Move Application List Entries: 400 when source list not open")
    void givenClosedSourceList_whenMove_then400() throws Exception {
        var token = getToken();

        ApplicationListPage page = getApplicationListPage(token, ApplicationListStatus.CLOSED);

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());

        // fire test
        Response resp =
                getMoveApplicationListEntriesResponse(
                        page.getContent().get(0).getId(),
                        page.getContent().get(1).getId(),
                        entryIds,
                        token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.INVALID_LIST_STATUS.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Move Application List Entries: 400 when target list not open")
    void givenClosedTargetList_whenMove_then400() throws Exception {
        var token = getToken();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(2),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        UUID sourceListId = page.getContent().get(0).getId();
        UUID targetListId = page.getContent().get(1).getId();

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());

        // fire test
        resp = getMoveApplicationListEntriesResponse(sourceListId, targetListId, entryIds, token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.INVALID_LIST_STATUS.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Move Application List Entries: 400 when entry unknown")
    void givenUnknownEntry_whenMove_then400() throws Exception {
        var token = getToken();

        ApplicationListPage page = getApplicationListPage(token, ApplicationListStatus.OPEN);

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());

        // fire test
        Response resp =
                getMoveApplicationListEntriesResponse(
                        page.getContent().get(0).getId(),
                        page.getContent().get(1).getId(),
                        entryIds,
                        token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Move Application List Entries: 400 when entry not in source list")
    void givenEntryNotInSourceList_whenMove_then400() throws Exception {
        var token = getToken();

        ApplicationListPage page = getApplicationListPage(token, ApplicationListStatus.OPEN);

        UUID sourceListId = page.getContent().get(0).getId();
        UUID targetListId = page.getContent().get(1).getId();
        UUID otherListId = page.getContent().get(2).getId();

        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + otherListId), token);
        ApplicationListGetDetailDto applicationListGetDetailDto =
                resp.as(ApplicationListGetDetailDto.class);

        Set<UUID> entryIds = new HashSet<>();
        UUID entry1Id = applicationListGetDetailDto.getEntriesSummary().getFirst().getUuid();
        entryIds.add(entry1Id);

        // fire test
        resp = getMoveApplicationListEntriesResponse(sourceListId, targetListId, entryIds, token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(UUID.randomUUID());

        var validPayload = new MoveEntriesDto().targetListId(UUID.randomUUID()).entryIds(entryIds);

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID() + "/entries/move"))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private ApplicationListPage getApplicationListPage(
            TokenAndJwksKey token, ApplicationListStatus open) throws MalformedURLException {
        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(2),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("status", open.toString()),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListPage.class);
    }

    private Response getMoveApplicationListEntriesResponse(
            UUID sourceListId, UUID targetListId, Set<UUID> entryIds, TokenAndJwksKey token)
            throws MalformedURLException {
        var req = new MoveEntriesDto().targetListId(targetListId).entryIds(entryIds);

        // fire test
        return restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/" + sourceListId + "/entries/move"), token, req);
    }
}
