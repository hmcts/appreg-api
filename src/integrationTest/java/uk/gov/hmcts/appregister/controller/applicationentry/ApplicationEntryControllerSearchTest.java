package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.appregister.common.security.RoleEnum.ADMIN;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewResponseDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;
import uk.gov.hmcts.appregister.generated.model.BulkActionType;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

class ApplicationEntryControllerSearchTest extends AbstractApplicationEntryCrudTest {

    @Autowired private ApplicationEntryService applicationEntryService;

    @Test
    void givenFilterSelection_whenBulkActionPreview_thenReturnMatchingIdsAndEntryContext()
            throws Exception {
        ApplicationList matchingList = createAndSaveList(Status.OPEN);
        ApplicationListEntry matchingEntry = createEntry(matchingList);
        matchingEntry.setAccountNumber("PV-FILTER-1");
        matchingEntry = persistance.save(matchingEntry);

        ApplicationList nonMatchingList = createAndSaveList(Status.OPEN);
        ApplicationListEntry nonMatchingEntry = createEntry(nonMatchingList);
        nonMatchingEntry.setAccountNumber("PV-FILTER-2");
        nonMatchingEntry = persistance.save(nonMatchingEntry);

        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setAccountReference("PV-FILTER-1");

        BulkActionPreviewResponseDto response =
                executeBulkActionPreview(
                        createAdminToken(),
                        bulkActionPreviewFilterRequest(
                                BulkActionType.UPDATE_NOTES,
                                filter,
                                List.of("date,desc"),
                                List.of()));

        Assertions.assertEquals(BulkActionType.UPDATE_NOTES, response.getAction());
        Assertions.assertEquals(2000, response.getLimit());
        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(matchingEntry.getUuid()), response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(matchingEntry.getUuid());
        assertThat(response.getEntries().getFirst().getAccountNumber().orElse(null))
                .isEqualTo("PV-FILTER-1");
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .doesNotContain(nonMatchingEntry.getUuid());
    }

    @Test
    void givenIdsSelection_whenBulkActionPreview_thenReturnSubmittedOrderAndEntryContext()
            throws Exception {
        ApplicationList firstList = createAndSaveList(Status.OPEN);
        ApplicationListEntry firstEntry = createEntry(firstList);
        firstEntry.setAccountNumber("PREVIEW-IDS-1");
        firstEntry = persistance.save(firstEntry);

        ApplicationList secondList = createAndSaveList(Status.OPEN);
        ApplicationListEntry secondEntry = createEntry(secondList);
        secondEntry.setAccountNumber("PREVIEW-IDS-2");
        secondEntry = persistance.save(secondEntry);

        BulkActionPreviewResponseDto response =
                executeBulkActionPreview(
                        createAdminToken(),
                        bulkActionPreviewIdsRequest(
                                BulkActionType.PRINT_PAGE,
                                List.of(secondEntry.getUuid(), firstEntry.getUuid())));

        Assertions.assertEquals(BulkActionType.PRINT_PAGE, response.getAction());
        Assertions.assertEquals(2, response.getSelectedCount());
        Assertions.assertEquals(2, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(
                List.of(secondEntry.getUuid(), firstEntry.getUuid()), response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(secondEntry.getUuid(), firstEntry.getUuid());
    }

    @Test
    void givenFilterSelectionWithExcludedEntryIds_whenBulkActionPreview_thenExcludeEntries()
            throws Exception {
        ApplicationList list = createAndSaveList(Status.OPEN);

        ApplicationListEntry includedEntry = createEntry(list);
        includedEntry.setAccountNumber("PV-EXCLUDE");
        includedEntry.setSequenceNumber((short) 1);
        includedEntry = persistance.save(includedEntry);

        ApplicationListEntry excludedEntry = createEntry(list);
        excludedEntry.setAccountNumber("PV-EXCLUDE");
        excludedEntry.setSequenceNumber((short) 2);
        excludedEntry = persistance.save(excludedEntry);

        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setAccountReference("PV-EXCLUDE");

        BulkActionPreviewResponseDto response =
                executeBulkActionPreview(
                        createAdminToken(),
                        bulkActionPreviewFilterRequest(
                                BulkActionType.PRINT_CONTINUOUS,
                                filter,
                                List.of(),
                                List.of(excludedEntry.getUuid())));

        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(includedEntry.getUuid()), response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(includedEntry.getUuid());
    }

    @Test
    void givenResultSelected_whenBulkActionPreview_thenReturnAllEntriesAndCountClosedIneligible()
            throws Exception {
        ApplicationList openList = createAndSaveList(Status.OPEN);
        ApplicationListEntry openEntry = createEntry(openList);
        openEntry.setAccountNumber("PV-RESULT");
        openEntry = persistance.save(openEntry);

        ApplicationList closedList = createAndSaveList(Status.CLOSED);
        ApplicationListEntry closedEntry = createEntry(closedList);
        closedEntry.setAccountNumber("PV-RESULT");
        closedEntry = persistance.save(closedEntry);

        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setAccountReference("PV-RESULT");

        BulkActionPreviewResponseDto response =
                executeBulkActionPreview(
                        createAdminToken(),
                        bulkActionPreviewFilterRequest(
                                BulkActionType.RESULT_SELECTED, filter, List.of(), List.of()));

        Assertions.assertEquals(2, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(1, response.getIneligibleCount());
        assertThat(response.getEntryIds())
                .containsExactlyInAnyOrder(openEntry.getUuid(), closedEntry.getUuid());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactlyInAnyOrder(openEntry.getUuid(), closedEntry.getUuid());
    }

    @Test
    void givenIdsSelectionAboveGlobalLimit_whenBulkActionPreview_thenReturnProblemJson()
            throws Exception {
        List<UUID> entryIds =
                LongStream.rangeClosed(1, 2001).mapToObj(value -> new UUID(0L, value)).toList();

        Response response =
                executeBulkActionPreviewResponse(
                        createAdminToken(),
                        bulkActionPreviewIdsRequest(BulkActionType.UPDATE_NOTES, entryIds));

        response.then().statusCode(413);
        assertThat(response.getHeader("Content-Type")).contains("application/problem+json");
        ProblemAssertUtil.assertEquals(
                AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT.getCode(), response);
    }

    @Test
    void givenIdsSelectionWithNullEntryId_whenBulkActionPreview_thenReturnProblemJson()
            throws Exception {
        restAssuredClient
                .executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/bulk-action-preview"),
                        createAdminToken().fetchTokenForRole(),
                        "{\"action\":\"UPDATE_NOTES\",\"selection\":{\"selectionType\":\"IDS\",\"entryIds\":[null]}}")
                .then()
                .statusCode(400);
    }

    @Test
    void givenListFilterSelection_whenListBulkActionPreview_thenReturnMatchingIdsAndContext()
            throws Exception {
        ApplicationList sourceList = createAndSaveList(Status.OPEN);

        ApplicationListEntry matchingEntry = createEntry(sourceList);
        matchingEntry.setAccountNumber("LIST-PV-FILTER");
        matchingEntry.setSequenceNumber((short) 1);
        matchingEntry = persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(sourceList);
        nonMatchingEntry.setAccountNumber("LIST-PV-OTHER");
        nonMatchingEntry.setSequenceNumber((short) 2);
        nonMatchingEntry = persistance.save(nonMatchingEntry);

        ApplicationList otherList = createAndSaveList(Status.OPEN);
        ApplicationListEntry otherListEntry = createEntry(otherList);
        otherListEntry.setAccountNumber("LIST-PV-FILTER");
        otherListEntry = persistance.save(otherListEntry);

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setAccountReference("LIST-PV-FILTER");

        BulkActionPreviewResponseDto response =
                executeApplicationListBulkActionPreview(
                        createAdminToken(),
                        sourceList.getUuid(),
                        applicationListBulkActionPreviewFilterRequest(
                                BulkActionType.UPDATE_OFFICIALS,
                                filter,
                                List.of("sequenceNumber,asc"),
                                List.of()));

        Assertions.assertEquals(BulkActionType.UPDATE_OFFICIALS, response.getAction());
        Assertions.assertEquals(1050, response.getLimit());
        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(matchingEntry.getUuid()), response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(matchingEntry.getUuid());
        assertThat(response.getEntries().getFirst().getAccountNumber().orElse(null))
                .isEqualTo("LIST-PV-FILTER");
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .doesNotContain(nonMatchingEntry.getUuid(), otherListEntry.getUuid());
    }

    @Test
    void givenListIdsSelectionUnderLimit_whenListBulkActionPreview_thenReturnSubmittedOrder()
            throws Exception {
        ApplicationList sourceList = createAndSaveList(Status.OPEN);

        ApplicationListEntry firstEntry = createEntry(sourceList);
        firstEntry.setAccountNumber("LIST-PV-IDS-1");
        firstEntry.setSequenceNumber((short) 1);
        firstEntry = persistance.save(firstEntry);

        ApplicationListEntry secondEntry = createEntry(sourceList);
        secondEntry.setAccountNumber("LIST-PV-IDS-2");
        secondEntry.setSequenceNumber((short) 2);
        secondEntry = persistance.save(secondEntry);

        BulkActionPreviewResponseDto response =
                executeApplicationListBulkActionPreview(
                        createAdminToken(),
                        sourceList.getUuid(),
                        applicationListBulkActionPreviewIdsRequest(
                                BulkActionType.MOVE_ENTRIES,
                                List.of(secondEntry.getUuid(), firstEntry.getUuid())));

        Assertions.assertEquals(BulkActionType.MOVE_ENTRIES, response.getAction());
        Assertions.assertEquals(1050, response.getLimit());
        Assertions.assertEquals(2, response.getSelectedCount());
        Assertions.assertEquals(2, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(
                List.of(secondEntry.getUuid(), firstEntry.getUuid()), response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(secondEntry.getUuid(), firstEntry.getUuid());
    }

    @Test
    void
            givenUpdateFeeDetailsSelection_whenListBulkActionPreview_thenOnlyFeeRequiredEntriesAreEligible()
                    throws Exception {
        final ApplicationList sourceList = createAndSaveList(Status.OPEN);

        ApplicationCode feeRequiredCode = buildApplicationCode("PVFEEYES");
        feeRequiredCode.setFeeDue(YesOrNo.YES);
        feeRequiredCode.setApplicationListEntryList(null);
        feeRequiredCode = persistance.save(feeRequiredCode);
        ApplicationListEntry feeRequiredEntry = createEntry(sourceList);
        feeRequiredEntry.setApplicationCode(feeRequiredCode);
        feeRequiredEntry.setSequenceNumber((short) 1);
        feeRequiredEntry = persistance.save(feeRequiredEntry);

        ApplicationCode feeNotRequiredCode = buildApplicationCode("PVFEENO");
        feeNotRequiredCode.setFeeDue(YesOrNo.NO);
        feeNotRequiredCode.setApplicationListEntryList(null);
        feeNotRequiredCode = persistance.save(feeNotRequiredCode);
        ApplicationListEntry feeNotRequiredEntry = createEntry(sourceList);
        feeNotRequiredEntry.setApplicationCode(feeNotRequiredCode);
        feeNotRequiredEntry.setSequenceNumber((short) 2);
        feeNotRequiredEntry = persistance.save(feeNotRequiredEntry);

        BulkActionPreviewResponseDto response =
                executeApplicationListBulkActionPreview(
                        createAdminToken(),
                        sourceList.getUuid(),
                        applicationListBulkActionPreviewIdsRequest(
                                BulkActionType.UPDATE_FEE_DETAILS,
                                List.of(
                                        feeRequiredEntry.getUuid(),
                                        feeNotRequiredEntry.getUuid())));

        Assertions.assertEquals(2, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(1, response.getIneligibleCount());
        Assertions.assertEquals(
                List.of(feeRequiredEntry.getUuid(), feeNotRequiredEntry.getUuid()),
                response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(feeRequiredEntry.getUuid(), feeNotRequiredEntry.getUuid());
    }

    @Test
    void givenListFilterSelectionWithExclusions_whenListBulkActionPreview_thenExcludeEntries()
            throws Exception {
        ApplicationList sourceList = createAndSaveList(Status.OPEN);

        ApplicationListEntry includedEntry = createEntry(sourceList);
        includedEntry.setAccountNumber("LIST-PV-EXCLUDE");
        includedEntry.setSequenceNumber((short) 1);
        includedEntry = persistance.save(includedEntry);

        ApplicationListEntry excludedEntry = createEntry(sourceList);
        excludedEntry.setAccountNumber("LIST-PV-EXCLUDE");
        excludedEntry.setSequenceNumber((short) 2);
        excludedEntry = persistance.save(excludedEntry);

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setAccountReference("LIST-PV-EXCLUDE");

        BulkActionPreviewResponseDto response =
                executeApplicationListBulkActionPreview(
                        createAdminToken(),
                        sourceList.getUuid(),
                        applicationListBulkActionPreviewFilterRequest(
                                BulkActionType.PRINT_CONTINUOUS,
                                filter,
                                List.of("sequenceNumber,asc"),
                                List.of(excludedEntry.getUuid())));

        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(includedEntry.getUuid()), response.getEntryIds());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(includedEntry.getUuid());
    }

    @Test
    void
            givenIdsSelectionFromAnotherList_whenApplicationListBulkActionPreview_thenReturnForbiddenProblemJson()
                    throws Exception {
        ApplicationList sourceList = createAndSaveList(Status.OPEN);
        ApplicationListEntry sourceEntry = createEntry(sourceList);
        sourceEntry = persistance.save(sourceEntry);

        ApplicationList otherList = createAndSaveList(Status.OPEN);
        ApplicationListEntry otherEntry = createEntry(otherList);
        otherEntry = persistance.save(otherEntry);

        Response response =
                executeApplicationListBulkActionPreviewResponse(
                        createAdminToken(),
                        sourceList.getUuid(),
                        applicationListBulkActionPreviewIdsRequest(
                                BulkActionType.UPDATE_FEE_DETAILS,
                                List.of(sourceEntry.getUuid(), otherEntry.getUuid())));

        response.then().statusCode(403);
        assertThat(response.getHeader("Content-Type")).contains("application/problem+json");
        ProblemDetail problemDetail = response.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(AppListEntryError.ENTRY_NOT_ACCESSIBLE_FOR_LIST.getCode().getAppCode());
        assertThat(problemDetail.getTitle())
                .isEqualTo(
                        "One or more application list entries do not belong to the application list");
        assertThat(problemDetail.getDetail()).contains(otherEntry.getUuid().toString());
    }

    @Test
    void givenListFilterSelectionAboveLimit_whenListBulkActionPreview_thenReturnProblemJson()
            throws Exception {
        int originalSingleListLimit = setSingleListBulkActionPreviewLimit(1);
        try {
            ApplicationList sourceList = createAndSaveList(Status.OPEN);

            ApplicationListEntry firstEntry = createEntry(sourceList);
            firstEntry.setAccountNumber("LIST-PV-LIMIT");
            firstEntry = persistance.save(firstEntry);

            ApplicationListEntry secondEntry = createEntry(sourceList);
            secondEntry.setAccountNumber("LIST-PV-LIMIT");
            secondEntry = persistance.save(secondEntry);

            EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
            filter.setAccountReference("LIST-PV-LIMIT");

            Response response =
                    executeApplicationListBulkActionPreviewResponse(
                            createAdminToken(),
                            sourceList.getUuid(),
                            applicationListBulkActionPreviewFilterRequest(
                                    BulkActionType.UPDATE_FEE_DETAILS,
                                    filter,
                                    List.of(),
                                    List.of()));

            response.then().statusCode(413);
            assertThat(response.getHeader("Content-Type")).contains("application/problem+json");
            ProblemAssertUtil.assertEquals(
                    AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT.getCode(), response);
        } finally {
            setSingleListBulkActionPreviewLimit(originalSingleListLimit);
        }
    }

    @Test
    void givenResultSelectedForClosedList_whenListBulkActionPreview_thenReturnIneligibleCount()
            throws Exception {
        ApplicationList closedList = createAndSaveList(Status.CLOSED);

        ApplicationListEntry closedEntry = createEntry(closedList);
        closedEntry.setAccountNumber("LIST-PV-RESULT");
        closedEntry = persistance.save(closedEntry);

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setAccountReference("LIST-PV-RESULT");

        BulkActionPreviewResponseDto response =
                executeApplicationListBulkActionPreview(
                        createAdminToken(),
                        closedList.getUuid(),
                        applicationListBulkActionPreviewFilterRequest(
                                BulkActionType.RESULT_SELECTED, filter, List.of(), List.of()));

        Assertions.assertEquals(BulkActionType.RESULT_SELECTED, response.getAction());
        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(0, response.getEligibleCount());
        Assertions.assertEquals(1, response.getIneligibleCount());
        assertThat(response.getEntryIds()).containsExactly(closedEntry.getUuid());
        assertThat(response.getEntries())
                .extracting(EntryGetSummaryDto::getId)
                .containsExactly(closedEntry.getUuid());
    }

    @Test
    void givenClosedStandardPersonApplicant_whenSearchByApplicantSurname_thenReturnMatchingEntry()
            throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        String applicantLastName = "DoeClosed" + suffix;

        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setStatus(ApplicationListStatus.CLOSED);
        filter.setApplicantSurname("doeclosed" + suffix);

        ApplicationListEntry matchingEntry =
                createClosedStandardPersonApplicantEntry(
                        "Jane", applicantLastName, "AC" + suffix.toUpperCase());
        EntryPage page = executeSearch(createAdminToken(), filter, maxPageSize);

        assertThat(page.getContent()).isNotNull();
        assertThat(page.getContent())
                .extracting(EntryGetSummaryDto::getId)
                .contains(matchingEntry.getUuid());

        EntryGetSummaryDto entry = findEntry(page, matchingEntry.getUuid());
        assertThat(entry.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);
        assertThat(entry.getApplicant().getPerson().getName().getFirstName()).isEqualTo("Jane");
        assertThat(entry.getApplicant().getPerson().getName().getLastName())
                .isEqualTo(applicantLastName);
    }

    @StabilityTest
    void testGetApplicationEntriesSearch() throws Exception {

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(ADMIN)).build();

        EntryPage page = executeSearch(tokenGenerator, null, 20);

        PagingAssertionUtil.assertPageDetails(page, 20, 0, 1, TOTAL_APP_ENTRY_COUNT);

        EntryGetSummaryDto entry = page.getContent().getFirst();

        assertThat(entry.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entry.getRespondent().getOrganisation().getName()).isEqualTo("Sarah Johnson");
        assertThat(entry.getApplicationTitle()).isEqualTo("Certified genuine copy document");

        dataAuditAssertionsForNoFilter();
    }

    @StabilityTest
    void testGetApplicationEntriesSearchWithAllDetails() throws Exception {

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(ADMIN)).build();

        EntryGetFilterDto filterDto = getEntryGetFilterDto();

        assertSingleTurnerSearchResult(tokenGenerator, filterDto);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        null,
                        "0",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "account_number",
                        null,
                        "29345",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "id",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "id",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_status",
                        null,
                        "OPEN",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "list_description",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "courthouse_code",
                        null,
                        "RCJ001",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "other_courthouse",
                        null,
                        "other",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_date",
                        null,
                        "2024-04-21",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_time",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        null,
                        "CJ",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "name",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "title",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "last_name",
                        null,
                        "Turner",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "postcode",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "name",
                        null,
                        "Sarah Johnson",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "title",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "last_name",
                        null,
                        "Turner",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "postcode",
                        null,
                        "XY9 8ZZ",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));
    }

    private static @NotNull EntryGetFilterDto getEntryGetFilterDto() {
        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setDate(LocalDate.parse("2024-04-21"));
        filterDto.setApplicantSurname("Turner");
        filterDto.setAccountReference("29345");
        filterDto.setStatus(ApplicationListStatus.OPEN);
        filterDto.setCjaCode("CJ");
        filterDto.setCourtCode("RCJ001");
        filterDto.setOtherLocationDescription("other");
        filterDto.setRespondentOrganisation("Sarah Johnson");
        filterDto.setRespondentPostcode("XY9 8ZZ");
        filterDto.setStandardApplicantCode("APP002");
        return filterDto;
    }

    @StabilityTest
    void testGetApplicationEntriesSearchWithPartialAllDetails() throws Exception {
        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setDate(LocalDate.parse("2024-04-21"));
        filterDto.setApplicantSurname("rn");
        filterDto.setAccountReference("29345");
        filterDto.setStatus(ApplicationListStatus.OPEN);
        filterDto.setCjaCode("CJ");
        filterDto.setCourtCode("RCJ001");
        filterDto.setOtherLocationDescription("her");
        filterDto.setRespondentOrganisation("ah Johnson");
        filterDto.setRespondentPostcode("XY9 8ZZ");
        filterDto.setStandardApplicantCode("APP0");

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(ADMIN)).build();
        assertSingleTurnerSearchResult(tokenGenerator, filterDto);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        null,
                        "0",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "account_number",
                        null,
                        "29345",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "id",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "id",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_status",
                        null,
                        "OPEN",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "list_description",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "courthouse_code",
                        null,
                        "RCJ001",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "other_courthouse",
                        null,
                        "her",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_date",
                        null,
                        "2024-04-21",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_time",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        null,
                        "CJ",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "name",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "title",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "last_name",
                        null,
                        "rn",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "postcode",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "name",
                        null,
                        "ah Johnson",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "title",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "last_name",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "postcode",
                        null,
                        "XY9 8ZZ",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));
    }

    @StabilityTest
    void givenApplicationEntryListSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
            throws Exception {

        for (ApplicationEntrySortFieldEnum sortField : ApplicationEntrySortFieldEnum.values()) {

            var tokenGenerator = createAdminToken();

            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(sortField.getApiValue() + ",desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());

            EntryPage page = responseSpec.as(EntryPage.class);

            responseSpec.then().statusCode(200);
            assertEquals(1, page.getSort().getOrders().size());
            assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().getFirst().getDirection());
            assertEquals(
                    sortField.getApiValue(), page.getSort().getOrders().getFirst().getProperty());

            dataAuditAssertionsForNoFilter();
        }

        Assertions.assertTrue(ApplicationEntrySortFieldEnum.values().length > 0);
    }

    @StabilityTest
    void givenSupportedSortKeys_whenGetApplicationEntries_thenSortBeforePaging() throws Exception {
        String uniqueToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String accountReferencePrefix = "SORT-" + uniqueToken + "-";
        SortFixture fixture = createGlobalSortFixture(uniqueToken, accountReferencePrefix);
        TokenGenerator tokenGenerator = createAdminToken();

        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.DATE,
                fixture.dateOrder());
        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.APPLICANT,
                fixture.applicantOrder());
        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.RESPONDENT,
                fixture.respondentOrder());
        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.APPLICATION_TITLE,
                fixture.applicationTitleOrder());
        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.FEE_REQUIRED,
                fixture.feeRequiredOrder());
        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.RESULTED,
                fixture.resultedOrder());
        assertPagedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                ApplicationEntrySortFieldEnum.STATUS,
                fixture.statusOrder());
    }

    @StabilityTest
    void givenResultedAndUnresultedEntries_whenGetApplicationEntriesSortedByIsResulted()
            throws Exception {
        String uniqueToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String accountReferencePrefix = "ISR-" + uniqueToken + "-";
        String applicationCodePrefix = "IR" + uniqueToken.substring(0, 5).toUpperCase();
        ApplicationList list = createAndSaveList(Status.OPEN);

        ApplicationListEntry unresulted =
                createEntryForIsResultedSort(
                        list, applicationCodePrefix + "A", accountReferencePrefix + "A");
        ApplicationListEntry resultedOne =
                createEntryForIsResultedSort(
                        list, applicationCodePrefix + "B", accountReferencePrefix + "B");
        ApplicationListEntry resultedTwo =
                createEntryForIsResultedSort(
                        list, applicationCodePrefix + "C", accountReferencePrefix + "C");

        addResolution(resultedOne, "IR1");
        addResolution(resultedTwo, "IR2");

        TokenGenerator tokenGenerator = createAdminToken();

        assertIsResultedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                SortableField.getSortStringForAsc(ApplicationEntrySortFieldEnum.IS_RESULTED),
                List.of(unresulted.getUuid(), resultedOne.getUuid(), resultedTwo.getUuid()));
        assertIsResultedSortOrder(
                tokenGenerator,
                accountReferencePrefix,
                SortableField.getSortStringForDesc(ApplicationEntrySortFieldEnum.IS_RESULTED),
                List.of(resultedTwo.getUuid(), resultedOne.getUuid(), unresulted.getUuid()));
    }

    @StabilityTest
    void
            givenValidRequest_whenGetApplicationEntriesWithPageNumberBeyondResultBoundary_thenReturn200()
                    throws Exception {

        var tokenGenerator = createAdminToken();

        int pageSize = 1;
        int pageNumber = 200;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(
                page, pageSize, pageNumber, TOTAL_APP_ENTRY_COUNT, TOTAL_APP_ENTRY_COUNT);
        Assertions.assertNotNull(page.getContent());
        Assertions.assertEquals(0, page.getContent().size());

        dataAuditAssertionsForNoFilter();
    }

    @StabilityTest
    void givenValidRequest_whenGetApplicationEntriesWithPagingInvalidSortQuery_thenReturn400()
            throws Exception {

        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of("invalid-sort"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);
        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), responseSpec);
    }

    @StabilityTest
    void givenCourtCodeSort_whenGetApplicationEntries_thenReturn400() throws Exception {
        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of("courtCode,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);
        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), responseSpec);
    }

    @StabilityTest
    void givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageNumber_thenReturn400()
            throws Exception {

        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(-1),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);
    }

    private SortFixture createGlobalSortFixture(String uniqueToken, String accountReferencePrefix) {
        String applicationCodePrefix = "S" + uniqueToken.substring(0, 5).toUpperCase();
        ApplicationListEntry alpha =
                createSortableEntry(
                        Status.CLOSED,
                        LocalDate.of(2024, Month.JANUARY, 3),
                        accountReferencePrefix + "A",
                        applicationCodePrefix + "A",
                        "Gamma Application",
                        YesOrNo.YES,
                        "Alice",
                        "Zebra",
                        "Carol",
                        "Able",
                        "M");
        ApplicationListEntry bravo =
                createSortableEntry(
                        Status.OPEN,
                        LocalDate.of(2024, Month.JANUARY, 1),
                        accountReferencePrefix + "B",
                        applicationCodePrefix + "B",
                        "Alpha Application",
                        YesOrNo.NO,
                        "Bob",
                        "Yellow",
                        "Alice",
                        "Baker",
                        "A");
        ApplicationListEntry charlie =
                createSortableEntry(
                        Status.OPEN,
                        LocalDate.of(2024, Month.JANUARY, 2),
                        accountReferencePrefix + "C",
                        applicationCodePrefix + "C",
                        "Beta Application",
                        YesOrNo.YES,
                        "Carol",
                        "Xavier",
                        "Bob",
                        "Cable",
                        "Z");

        return new SortFixture(
                List.of(bravo.getUuid(), charlie.getUuid(), alpha.getUuid()),
                List.of(alpha.getUuid(), bravo.getUuid(), charlie.getUuid()),
                List.of(bravo.getUuid(), charlie.getUuid(), alpha.getUuid()),
                List.of(bravo.getUuid(), charlie.getUuid(), alpha.getUuid()),
                List.of(bravo.getUuid(), alpha.getUuid(), charlie.getUuid()),
                List.of(bravo.getUuid(), alpha.getUuid(), charlie.getUuid()),
                List.of(alpha.getUuid(), bravo.getUuid(), charlie.getUuid()));
    }

    private ApplicationListEntry createSortableEntry(
            Status status,
            LocalDate date,
            String accountReference,
            String applicationCodeValue,
            String applicationTitle,
            YesOrNo feeRequired,
            String applicantFirstName,
            String applicantLastName,
            String respondentFirstName,
            String respondentLastName,
            String resultCode) {
        ApplicationList list = createAndSaveList(status);
        list.setDate(date);
        persistance.save(list);

        ApplicationCode applicationCode = createApplicationCode(applicationCodeValue, true);
        applicationCode.setTitle(applicationTitle);
        applicationCode.setFeeDue(feeRequired);
        applicationCode.setApplicationListEntryList(null);
        applicationCode = persistance.save(applicationCode);
        applicationCode.setApplicationListEntryList(null);

        ApplicationListEntry entry = createEntry(list);
        entry.setApplicationCode(applicationCode);
        entry.setAccountNumber(accountReference);
        setApplicantName(entry, "Mx", applicantFirstName, applicantLastName);
        setRespondentName(entry, "Mx", respondentFirstName, respondentLastName);
        entry.getAnamedaddress().setName(null);
        entry.getRnameaddress().setName(null);
        persistance.save(entry.getAnamedaddress());
        persistance.save(entry.getRnameaddress());
        entry = persistance.save(entry);
        addResolution(entry, resultCode);

        return entry;
    }

    private ApplicationListEntry createEntryForIsResultedSort(
            ApplicationList list, String applicationCodeValue, String accountReference) {
        ApplicationCode applicationCode = createApplicationCode(applicationCodeValue, true);
        applicationCode.setApplicationListEntryList(null);

        ApplicationListEntry entry = createEntry(list);
        entry.setApplicationCode(applicationCode);
        entry.setAccountNumber(accountReference);
        return persistance.save(entry);
    }

    private void assertIsResultedSortOrder(
            TokenGenerator tokenGenerator,
            String accountReferencePrefix,
            String sort,
            List<UUID> expectedOrder)
            throws Exception {
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(3),
                        Optional.of(0),
                        List.of(sort),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("accountReference", accountReferencePrefix),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 3, 0, 1, 3);
        Assertions.assertEquals(
                expectedOrder, page.getContent().stream().map(EntryGetSummaryDto::getId).toList());
    }

    private void assertPagedSortOrder(
            TokenGenerator tokenGenerator,
            String accountReferencePrefix,
            ApplicationEntrySortFieldEnum sortField,
            List<UUID> expectedOrder)
            throws Exception {
        for (int pageNumber = 0; pageNumber < expectedOrder.size(); pageNumber++) {
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(1),
                            Optional.of(pageNumber),
                            List.of(SortableField.getSortStringForAsc(sortField)),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole(),
                            rs -> rs.queryParam("accountReference", accountReferencePrefix),
                            new OpenApiPageMetaData());

            responseSpec.then().statusCode(200);
            EntryPage page = responseSpec.as(EntryPage.class);
            PagingAssertionUtil.assertPageDetails(page, 1, pageNumber, 3, 3);
            Assertions.assertEquals(
                    expectedOrder.get(pageNumber), page.getContent().getFirst().getId());
        }
    }

    private record SortFixture(
            List<UUID> dateOrder,
            List<UUID> applicantOrder,
            List<UUID> respondentOrder,
            List<UUID> applicationTitleOrder,
            List<UUID> feeRequiredOrder,
            List<UUID> resultedOrder,
            List<UUID> statusOrder) {}

    @StabilityTest
    void
            givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageSizeBeyondDefault_thenReturn400()
                    throws Exception {

        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize + 1),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);
    }

    @Test
    void givenValidRequest_whenMultipleSortsArePresent_thenReturn400() throws Exception {
        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize),
                        Optional.of(0),
                        List.of(
                                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue()
                                        + ",asc",
                                ApplicationEntrySortFieldEnum.APPLICANT.getApiValue() + ",asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
    }

    @Test
    void givenInvalidRespondentPostcodeFilter_whenGetApplicationEntries_thenReturn400()
            throws Exception {
        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentPostcode", "@£1 1@£"));

        responseSpec.then().statusCode(400);
        responseSpec
                .then()
                .body("errors.respondentPostcode", Matchers.containsString("must match"));
    }

    @Test
    void givenPartialRespondentPostcodeMatch_whenGetApplicationEntries_thenReturnMatchingEntry()
            throws Exception {
        ApplicationList matchingList = createAndSaveList(Status.OPEN);

        ApplicationListEntry matchingEntry = createEntry(matchingList);
        setRespondentName(matchingEntry, "Mr", "Partial", "Match");
        matchingEntry.getRnameaddress().setPostcode("SW1A 1AA");
        matchingEntry = persistance.save(matchingEntry);

        ApplicationList nonMatchingList = createAndSaveList(Status.OPEN);

        ApplicationListEntry nonMatchingEntry = createEntry(nonMatchingList);
        setRespondentName(nonMatchingEntry, "Ms", "Partial", "Miss");
        nonMatchingEntry.getRnameaddress().setPostcode("XY9 8ZZ");
        persistance.save(nonMatchingEntry);

        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentPostcode", "sw1"));

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    void givenPartialRespondentPostcodeMiss_whenGetApplicationEntries_thenReturnEmptyPage()
            throws Exception {
        ApplicationList list = createAndSaveList(Status.OPEN);

        ApplicationListEntry entry = createEntry(list);
        setRespondentName(entry, "Mr", "Partial", "Miss");
        entry.getRnameaddress().setPostcode("SW1A 1AA");
        persistance.save(entry);

        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentPostcode", "ZZ9"));

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertNotNull(page.getContent());
        Assertions.assertEquals(0, page.getContent().size());
    }

    @Test
    @StabilityTest
    void testGetApplicationEntriesSearchReturnsAllResultCodes() throws Exception {
        ApplicationList list = createAndSaveList(Status.OPEN);
        ApplicationCode applicationCode = createApplicationCode("APP002", false);

        ApplicationListEntry entry = createEntry(list);
        entry.setApplicationCode(applicationCode);
        entry.setAccountNumber("RESULT-12345");
        entry = persistance.save(entry);

        saveResolutions(entry, "RC1", "RC2");

        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setAccountReference("RESULT-12345");

        TokenGenerator tokenGenerator = createAdminToken();
        EntryPage page = executeSearch(tokenGenerator, filterDto, 20);

        assertThat(page.getContent()).isNotNull();
        assertEquals(1, page.getContent().size());

        EntryGetSummaryDto dto = page.getContent().getFirst();

        assertResultCodes(dto, "RC1", "RC2");
    }

    /** Executes search with optional filter and returns EntryPage. */
    private EntryPage executeSearch(
            TokenGenerator tokenGenerator, EntryGetFilterDto filterDto, int size) throws Exception {

        UnaryOperator<RequestSpecification> filterOperator =
                filterDto == null
                        ? UnaryOperator.identity()
                        : new ApplicationEntryFilter(
                                Optional.ofNullable(filterDto.getDate()),
                                Optional.ofNullable(filterDto.getCourtCode()),
                                Optional.ofNullable(filterDto.getOtherLocationDescription()),
                                Optional.ofNullable(filterDto.getCjaCode()),
                                Optional.ofNullable(filterDto.getApplicantOrganisation()),
                                Optional.ofNullable(filterDto.getApplicantSurname()),
                                Optional.ofNullable(
                                        filterDto.getStatus() == null
                                                ? null
                                                : filterDto.getStatus().toString()),
                                Optional.ofNullable(filterDto.getRespondentOrganisation()),
                                Optional.ofNullable(filterDto.getRespondentSurname()),
                                Optional.ofNullable(filterDto.getRespondentPostcode()),
                                Optional.ofNullable(filterDto.getAccountReference()),
                                Optional.ofNullable(filterDto.getStandardApplicantCode()));

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(size),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        filterOperator,
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        return responseSpec.as(EntryPage.class);
    }

    private BulkActionPreviewResponseDto executeBulkActionPreview(
            TokenGenerator tokenGenerator, BulkActionPreviewRequestDto request) throws Exception {
        Response response = executeBulkActionPreviewResponse(tokenGenerator, request);

        response.then().statusCode(200);
        return response.as(BulkActionPreviewResponseDto.class);
    }

    private Response executeBulkActionPreviewResponse(
            TokenGenerator tokenGenerator, BulkActionPreviewRequestDto request) throws Exception {
        return restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/bulk-action-preview"),
                tokenGenerator.fetchTokenForRole(),
                request);
    }

    private Response executeApplicationListBulkActionPreviewResponse(
            TokenGenerator tokenGenerator,
            UUID listId,
            ApplicationListEntryBulkActionPreviewRequestDto request)
            throws Exception {
        return restAssuredClient.executePostRequest(
                getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-action-preview"),
                tokenGenerator.fetchTokenForRole(),
                request);
    }

    private BulkActionPreviewResponseDto executeApplicationListBulkActionPreview(
            TokenGenerator tokenGenerator,
            UUID listId,
            ApplicationListEntryBulkActionPreviewRequestDto request)
            throws Exception {
        Response response =
                executeApplicationListBulkActionPreviewResponse(tokenGenerator, listId, request);

        response.then().statusCode(200);
        return response.as(BulkActionPreviewResponseDto.class);
    }

    private BulkActionPreviewRequestDto bulkActionPreviewFilterRequest(
            BulkActionType action,
            EntryGetFilterDto filter,
            List<String> sort,
            List<UUID> excludedEntryIds) {
        return new BulkActionPreviewRequestDto()
                .action(action)
                .selection(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.FILTER)
                                .filter(filter)
                                .sort(sort)
                                .excludedEntryIds(excludedEntryIds));
    }

    private BulkActionPreviewRequestDto bulkActionPreviewIdsRequest(
            BulkActionType action, List<UUID> entryIds) {
        return new BulkActionPreviewRequestDto()
                .action(action)
                .selection(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(entryIds));
    }

    private ApplicationListEntryBulkActionPreviewRequestDto
            applicationListBulkActionPreviewIdsRequest(BulkActionType action, List<UUID> entryIds) {
        return new ApplicationListEntryBulkActionPreviewRequestDto()
                .action(action)
                .selection(
                        new ApplicationListEntryBulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(entryIds));
    }

    private ApplicationListEntryBulkActionPreviewRequestDto
            applicationListBulkActionPreviewFilterRequest(
                    BulkActionType action,
                    EntryApplicationListGetFilterDto filter,
                    List<String> sort,
                    List<UUID> excludedEntryIds) {
        return new ApplicationListEntryBulkActionPreviewRequestDto()
                .action(action)
                .selection(
                        new ApplicationListEntryBulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.FILTER)
                                .filter(filter)
                                .sort(sort)
                                .excludedEntryIds(excludedEntryIds));
    }

    private int setSingleListBulkActionPreviewLimit(int limit) throws Exception {
        Object target = AopTestUtils.getTargetObject(applicationEntryService);
        Integer previousLimit =
                (Integer) ReflectionTestUtils.getField(target, "bulkActionPreviewSingleListLimit");

        ReflectionTestUtils.setField(target, "bulkActionPreviewSingleListLimit", limit);
        return previousLimit;
    }

    /** Executes search and asserts the expected single Turner result. */
    private void assertSingleTurnerSearchResult(
            TokenGenerator tokenGenerator, EntryGetFilterDto filterDto) throws Exception {

        EntryPage page = executeSearch(tokenGenerator, filterDto, 10);

        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);

        EntryGetSummaryDto entry = page.getContent().getFirst();

        assertThat(entry.getApplicant().getPerson().getName().getLastName()).isEqualTo("Turner");
        assertThat(entry.getIsFeeRequired()).isTrue();
        assertThat(entry.getIsResulted()).isTrue();
    }

    private void dataAuditAssertionsForNoFilter() {
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        null,
                        "0",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "account_number",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "id",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "id",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_status",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "list_description",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "courthouse_code",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "other_courthouse",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_date",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "application_list_time",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_code",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_description",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "name",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "title",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "last_name",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "postcode",
                        null,
                        "",
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST.getEventName()));
    }

    private ApplicationListEntry createClosedStandardPersonApplicantEntry(
            String applicantFirstName, String applicantLastName, String accountReference) {
        StandardApplicant applicant = new StandardApplicantTestData().someComplete();
        applicant.setApplicantCode("SA" + accountReference);
        applicant.setApplicantStartDate(TEST_DATE);
        applicant.setApplicantEndDate(null);
        applicant.setName(null);
        applicant.setApplicantTitle("Ms");
        applicant.setApplicantForename1(applicantFirstName);
        applicant.setApplicantSurname(applicantLastName);
        applicant.setAddressLine1("1 Closed Road");
        applicant.setChangedBy(1L);
        applicant.setChangedDate(TEST_OFFSET_DATE_TIME);
        applicant.setCreatedUser("email");
        applicant = persistance.save(applicant);

        ApplicationCode applicationCode = buildApplicationCode("CD" + accountReference);
        applicationCode.setTitle("Condemnation of Unfit Food");
        applicationCode.setApplicationListEntryList(null);
        applicationCode = persistance.save(applicationCode);

        ApplicationList list = createAndSaveList(Status.CLOSED);
        ApplicationListEntry entry = createEntry(list);
        entry.setAnamedaddress(null);
        entry.setStandardApplicant(applicant);
        entry.setApplicationCode(applicationCode);
        entry.setAccountNumber(accountReference);
        entry.setSequenceNumber((short) 1);
        return persistance.save(entry);
    }
}
