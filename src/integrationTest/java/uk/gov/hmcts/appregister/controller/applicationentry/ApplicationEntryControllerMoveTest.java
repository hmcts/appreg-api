package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.YesOrNo.YES;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.controller.applicationcode.AbstractApplicationCodeEntryCrudTest;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

public class ApplicationEntryControllerMoveTest extends AbstractApplicationCodeEntryCrudTest {
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

        // a date that is without range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);
    }

    @Test
    @DisplayName("Move Application List Entries")
    void givenValidRequest_whenMove_then200() throws Exception {
        ApplicationListEntry sourceEntry = new AppListEntryTestData().someMinimal().build();

        ApplicationList sourceList = createOpenListWithEntry(sourceEntry);

        ApplicationList targetList = createOpenTargetList();

        Response resp = moveEntries(sourceList, targetList, Set.of(sourceEntry.getUuid()));

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
                        new OpenApiPageMetaData());

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

    @Test
    @DisplayName("Move Application List Entries: 400 when entry is deleted")
    void givenDeletedEntry_whenMove_then400() throws Exception {
        ApplicationListEntry deletedEntry = new AppListEntryTestData().someMinimal().build();
        deletedEntry.setDeleted(YES);

        ApplicationList sourceList = createOpenListWithEntry(deletedEntry);

        ApplicationList targetList = createOpenTargetList();

        Response resp = moveEntries(sourceList, targetList, Set.of(deletedEntry.getUuid()));

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName(
            "Move Application List Entries: 400 with invalid_entry_ids when some entries are missing")
    void givenMixedValidAndInvalidEntries_whenMove_then400WithInvalidIds() throws Exception {
        var token = getToken();

        ApplicationListPage page = getApplicationListPage(token, ApplicationListStatus.OPEN);

        UUID sourceListId = page.getContent().get(0).getId();
        UUID targetListId = page.getContent().get(1).getId();
        UUID otherListId = page.getContent().get(2).getId();

        Response sourceResp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + sourceListId), token);

        ApplicationListGetDetailDto sourceDetail = sourceResp.as(ApplicationListGetDetailDto.class);

        UUID validEntryId = sourceDetail.getEntriesSummary().getFirst().getUuid();

        Response otherResp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + otherListId), token);

        ApplicationListGetDetailDto otherDetail = otherResp.as(ApplicationListGetDetailDto.class);

        UUID invalidEntryId = otherDetail.getEntriesSummary().getFirst().getUuid();

        Set<UUID> entryIds = new HashSet<>();
        entryIds.add(validEntryId);
        entryIds.add(invalidEntryId);

        // fire request
        Response resp =
                getMoveApplicationListEntriesResponse(sourceListId, targetListId, entryIds, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);

        Assertions.assertNotNull(problemDetail.getDetail());
        Assertions.assertTrue(problemDetail.getDetail().contains(invalidEntryId.toString()));
    }

    private ApplicationListPage getApplicationListPage(
            TokenAndJwksKey token, ApplicationListStatus open) throws Exception {
        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(3),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("status", open.toString()),
                        new OpenApiPageMetaData());

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

    private ApplicationList createOpenListWithEntry(ApplicationListEntry entry) {
        ApplicationList list = new AppListTestData().someMinimal().build();
        entry.setApplicationList(list);

        persistance.save(entry);
        list.setStatus(Status.OPEN);
        persistance.save(list);

        return list;
    }

    private ApplicationList createOpenTargetList() {
        ApplicationList targetList = new AppListTestData().someMinimal().build();
        ApplicationListEntry targetEntry = new AppListEntryTestData().someMinimal().build();
        targetEntry.setApplicationList(targetList);

        persistance.save(targetEntry);
        targetList.setStatus(Status.OPEN);
        persistance.save(targetList);

        return targetList;
    }

    private Response moveEntries(
            ApplicationList sourceList, ApplicationList targetList, Set<UUID> uuidsToMove)
            throws Exception {
        return getMoveApplicationListEntriesResponse(
                sourceList.getUuid(), targetList.getUuid(), uuidsToMove, getToken());
    }
}
