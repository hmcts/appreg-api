package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.YesOrNo.YES;

import io.restassured.response.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.controller.applicationcode.AbstractApplicationCodeEntryCrudTest;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

public class ApplicationEntryControllerMoveTest extends AbstractApplicationCodeEntryCrudTest {
    @MockitoBean private UserProvider provider;
    @Autowired private DataAuditRepository dataAuditRepository;
    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;
    @Autowired private MoveEntryFailureSwitch moveEntryFailureSwitch;
    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    private static final String UNKNOWN_APPLICATION_LIST_ID =
            "ffffffff-ffff-ffff-ffff-ffffffffffff";

    @BeforeEach
    public void before() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
        moveEntryFailureSwitch.reset();

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

        Response targetResp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + targetList.getUuid()), getToken());

        targetResp.then().statusCode(HttpStatus.OK.value());

        ApplicationListGetDetailDto targetDetail = targetResp.as(ApplicationListGetDetailDto.class);

        Assertions.assertTrue(
                targetDetail.getEntriesSummary().stream()
                        .anyMatch(e -> e.getUuid().equals(sourceEntry.getUuid())));

        Assertions.assertEquals(2, targetDetail.getEntriesSummary().size());

        var sequences =
                targetDetail.getEntriesSummary().stream()
                        .map(ApplicationListEntrySummary::getSequenceNumber)
                        .sorted()
                        .toList();

        Assertions.assertEquals(2, sequences.size());
        Assertions.assertTrue(sequences.get(0) < sequences.get(1));
    }

    @Test
    @DisplayName("Move Application List Entries: persists data audit rows")
    void givenValidRequest_whenMove_thenDataAuditRowsPersisted() throws Exception {
        val sourceEntry = new AppListEntryTestData().someMinimal().build();
        val sourceList = createOpenListWithEntry(sourceEntry);
        val targetList = createOpenTargetList();

        // Re-read the persisted entry so we can compare the version before and after the move.
        val persistedBeforeMove =
                applicationListEntryRepository.findByUuid(sourceEntry.getUuid()).orElseThrow();

        // Clear earlier audit rows so these assertions only inspect the move request.
        dataAuditRepository.deleteAll();

        val originalVersion = persistedBeforeMove.getVersion();
        val resp = moveEntries(sourceList, targetList, Set.of(sourceEntry.getUuid()));

        resp.then().statusCode(HttpStatus.OK.value());

        val persistedAfterMove =
                applicationListEntryRepository.findByUuid(sourceEntry.getUuid()).orElseThrow();

        // The entry should now belong to the target list and have an incremented version.
        Assertions.assertEquals(
                targetList.getId(), persistedAfterMove.getApplicationList().getId());
        Assertions.assertTrue(persistedAfterMove.getVersion() > originalVersion);

        // The move audit should capture the old and new list identifiers for the moved entry.
        val listIdAuditRow =
                dataAuditRepository.findAll().stream()
                        .filter(row -> TableNames.APPLICATION_LISTS.equals(row.getTableName()))
                        .filter(row -> "al_id".equals(row.getColumnName()))
                        .filter(row -> sourceList.getId().toString().equals(row.getOldValue()))
                        .filter(row -> targetList.getId().toString().equals(row.getNewValue()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an application_lists.al_id move audit row"));

        Assertions.assertEquals(
                AppListEntryAuditOperation.MOVE_APP_ENTRY.getEventName(),
                listIdAuditRow.getEventName());
        Assertions.assertEquals(
                AppListEntryAuditOperation.MOVE_APP_ENTRY.getType(),
                listIdAuditRow.getUpdateType());

        // The same move request should also audit the version increment on the entry row itself.
        val versionAuditRow =
                dataAuditRepository.findAll().stream()
                        .filter(
                                row ->
                                        TableNames.APPLICATION_LISTS_ENTRY.equals(
                                                row.getTableName()))
                        .filter(row -> "version".equals(row.getColumnName()))
                        .filter(row -> Long.toString(originalVersion).equals(row.getOldValue()))
                        .filter(
                                row ->
                                        Long.toString(persistedAfterMove.getVersion())
                                                .equals(row.getNewValue()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an application_list_entries.version move audit row"));

        Assertions.assertEquals(
                AppListEntryAuditOperation.MOVE_APP_ENTRY.getEventName(),
                versionAuditRow.getEventName());
        Assertions.assertEquals(
                AppListEntryAuditOperation.MOVE_APP_ENTRY.getType(),
                versionAuditRow.getUpdateType());
    }

    @Test
    @DisplayName("Move Application List Entries: rolls back all entries when one save fails")
    void givenSecondMoveSaveFails_whenMove_thenAllEntriesRollBack() throws Exception {
        val firstEntry = new AppListEntryTestData().someMinimal().build();
        val sourceList = createOpenListWithEntry(firstEntry);

        val secondEntry = new AppListEntryTestData().someMinimal().build();
        secondEntry.setApplicationList(sourceList);
        persistance.save(secondEntry);

        val targetList = createOpenTargetList();

        // Clear earlier audit rows so we only inspect what this failing move attempted to write.
        dataAuditRepository.deleteAll();

        // Fail on the second save call inside the move loop so we can prove the first move rolls
        // back as part of the same transaction.
        moveEntryFailureSwitch.failOnSecondSave();

        try {
            val resp =
                    moveEntries(
                            sourceList,
                            targetList,
                            Set.of(firstEntry.getUuid(), secondEntry.getUuid()));

            resp.then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } finally {
            moveEntryFailureSwitch.reset();
        }

        // Re-read both entries from the database. If the transaction rolled back correctly they
        // must still belong to the original source list, despite the first save happening before
        // the forced failure.
        val persistedFirst =
                applicationListEntryRepository.findByUuid(firstEntry.getUuid()).orElseThrow();
        val persistedSecond =
                applicationListEntryRepository.findByUuid(secondEntry.getUuid()).orElseThrow();

        Assertions.assertEquals(sourceList.getId(), persistedFirst.getApplicationList().getId());
        Assertions.assertEquals(sourceList.getId(), persistedSecond.getApplicationList().getId());

        // The audit rows for the move must also roll back with the business transaction.
        Assertions.assertTrue(
                dataAuditRepository.findAll().stream()
                        .noneMatch(
                                row ->
                                        AppListEntryAuditOperation.MOVE_APP_ENTRY
                                                .getEventName()
                                                .equals(row.getEventName())));
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
        Assertions.assertNotNull(problemDetail.getType());
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

    @TestConfiguration
    static class MoveEntryRollbackTestConfig {
        @Bean
        MoveEntryFailureSwitch moveEntryFailureSwitch() {
            return new MoveEntryFailureSwitch();
        }

        @Bean
        BeanPostProcessor moveEntryRepositoryFailureInjector(MoveEntryFailureSwitch switcher) {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName)
                        throws BeansException {
                    if (!(bean instanceof ApplicationListEntryRepository repository)) {
                        return bean;
                    }

                    return Proxy.newProxyInstance(
                            bean.getClass().getClassLoader(),
                            bean.getClass().getInterfaces(),
                            (proxy, method, args) -> {
                                if ("save".equals(method.getName())
                                        && args != null
                                        && args.length == 1
                                        && switcher.shouldFail(args[0])) {
                                    throw new IllegalStateException(
                                            "Simulated move save failure for rollback test");
                                }

                                try {
                                    return method.invoke(repository, args);
                                } catch (InvocationTargetException ex) {
                                    throw ex.getTargetException();
                                }
                            });
                }
            };
        }
    }

    static class MoveEntryFailureSwitch {
        private final AtomicBoolean enabled = new AtomicBoolean(false);
        private final AtomicInteger saveCount = new AtomicInteger(0);

        void failOnSecondSave() {
            saveCount.set(0);
            enabled.set(true);
        }

        void reset() {
            enabled.set(false);
            saveCount.set(0);
        }

        boolean shouldFail(Object candidate) {
            return enabled.get()
                    && candidate instanceof ApplicationListEntry
                    && saveCount.incrementAndGet() == 2;
        }
    }
}
