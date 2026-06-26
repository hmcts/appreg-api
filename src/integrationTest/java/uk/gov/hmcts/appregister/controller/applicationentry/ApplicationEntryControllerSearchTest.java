package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.appregister.common.security.RoleEnum.ADMIN;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryIdsDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

class ApplicationEntryControllerSearchTest extends AbstractApplicationEntryCrudTest {

    @Test
    void givenExistingList_whenGetApplicationListEntryIdsWithoutFilters_thenReturnAllIds()
            throws Exception {
        UUID listId = getOpenApplicationListId();

        EntryPage page = executeGetEntries(listId, 100, 0).as(EntryPage.class);
        EntryIdsDto response = executeListEntryIdsSearch(createAdminToken(), listId, null);

        Assertions.assertNotNull(response.getIds());
        List<UUID> expectedIds = page.getContent().stream().map(EntryGetSummaryDto::getId).toList();
        Assertions.assertEquals(expectedIds.size(), response.getIds().size());
        Assertions.assertTrue(response.getIds().containsAll(expectedIds));
    }

    @Test
    void givenMatchingFilter_whenGetApplicationListEntryIds_thenReturnOnlyMatchingIds()
            throws Exception {
        final var list = createAndSaveList(Status.OPEN);

        var matchingApplicant = new NameAddressTestData().someOrganisation();
        matchingApplicant.setCode(NameAddressCodeType.APPLICANT);
        matchingApplicant.setName("Filter Match Org");
        matchingApplicant = persistance.save(matchingApplicant);

        var nonMatchingApplicant = new NameAddressTestData().someOrganisation();
        nonMatchingApplicant.setCode(NameAddressCodeType.APPLICANT);
        nonMatchingApplicant.setName("Different Org");
        nonMatchingApplicant = persistance.save(nonMatchingApplicant);

        var applicationCode = createApplicationCode("APPIDSFLT", true);

        var matchingEntry = createEntry(list);
        matchingEntry.setApplicationCode(applicationCode);
        matchingEntry.setAnamedaddress(matchingApplicant);
        matchingEntry.setSequenceNumber((short) 1);
        matchingEntry = persistance.save(matchingEntry);
        applicationCode =
                createApplicationCodeCopy(
                        applicationCodeRepository.findById(applicationCode.getId()).orElseThrow());

        var nonMatchingEntry = createEntry(list);
        nonMatchingEntry.setApplicationCode(applicationCode);
        nonMatchingEntry.setAnamedaddress(nonMatchingApplicant);
        nonMatchingEntry.setSequenceNumber((short) 2);
        nonMatchingEntry = persistance.save(nonMatchingEntry);

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setApplicantName("Match Org");

        EntryIdsDto response =
                executeListEntryIdsSearch(createAdminToken(), list.getUuid(), filter);

        Assertions.assertEquals(List.of(matchingEntry.getUuid()), response.getIds());
        assertThat(response.getIds()).doesNotContain(nonMatchingEntry.getUuid());
    }

    @Test
    void givenPartialResultedFilter_whenGetApplicationListEntryIds_thenReturnMatchingIds()
            throws Exception {
        final var list = createAndSaveList(Status.OPEN);

        var matchingEntry = createEntry(list);
        matchingEntry.setApplicationCode(createApplicationCode("APPIDSR1", true));
        matchingEntry.setSequenceNumber((short) 1);
        matchingEntry = persistance.save(matchingEntry);
        saveResolutions(matchingEntry, "APPC");

        var nonMatchingEntry = createEntry(list);
        nonMatchingEntry.setApplicationCode(createApplicationCode("APPIDSR2", true));
        nonMatchingEntry.setSequenceNumber((short) 2);
        nonMatchingEntry = persistance.save(nonMatchingEntry);
        saveResolutions(nonMatchingEntry, "RC1");

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setResulted("AP");

        EntryIdsDto response =
                executeListEntryIdsSearch(createAdminToken(), list.getUuid(), filter);

        Assertions.assertEquals(List.of(matchingEntry.getUuid()), response.getIds());
        assertThat(response.getIds()).doesNotContain(nonMatchingEntry.getUuid());
    }

    @Test
    void givenNoMatches_whenGetApplicationListEntryIds_thenReturnEmptyList() throws Exception {
        UUID listId = getOpenApplicationListId();

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setApplicantName("definitely-no-match-for-entry-ids");

        EntryIdsDto response = executeListEntryIdsSearch(createAdminToken(), listId, filter);

        Assertions.assertNotNull(response.getIds());
        assertThat(response.getIds()).isEmpty();
    }

    @Test
    void givenMoreThanOnePageOfMatches_whenGetApplicationListEntryIds_thenReturnAllIds()
            throws Exception {
        final var list = createAndSaveList(Status.OPEN);
        List<UUID> expectedIds = new ArrayList<>();
        var applicationCode = createApplicationCode("APPIDSMUL", true);

        for (short i = 1; i <= 12; i++) {
            var entry = createEntry(list);
            entry.setApplicationCode(applicationCode);
            entry.setSequenceNumber(i);
            entry.setAccountNumber("MULTIPAGE-" + i);
            entry = persistance.save(entry);
            applicationCode =
                    createApplicationCodeCopy(
                            applicationCodeRepository
                                    .findById(applicationCode.getId())
                                    .orElseThrow());
            expectedIds.add(entry.getUuid());
        }

        EntryPage pagedResponse =
                restAssuredClient
                        .executeGetRequestWithPaging(
                                Optional.of(10),
                                Optional.of(0),
                                List.of(),
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                                createAdminToken().fetchTokenForRole(),
                                rs -> rs.queryParam("accountReference", "MULTIPAGE-"),
                                new OpenApiPageMetaData())
                        .as(EntryPage.class);

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setAccountReference("MULTIPAGE-");

        EntryIdsDto response =
                executeListEntryIdsSearch(createAdminToken(), list.getUuid(), filter);

        Assertions.assertEquals(10, pagedResponse.getContent().size());
        Assertions.assertEquals(12, pagedResponse.getTotalElements());
        Assertions.assertEquals(expectedIds.size(), response.getIds().size());
        Assertions.assertTrue(response.getIds().containsAll(expectedIds));
    }

    @Test
    void givenExistingEntries_whenGetEntryIdsWithoutFilters_thenReturnAllIds() throws Exception {
        EntryPage page = executeSearch(createAdminToken(), null, 100);
        EntryIdsDto response = executeGlobalEntryIdsSearch(createAdminToken(), null);

        Assertions.assertNotNull(response.getIds());
        List<UUID> expectedIds = page.getContent().stream().map(EntryGetSummaryDto::getId).toList();
        Assertions.assertEquals(expectedIds.size(), response.getIds().size());
        Assertions.assertTrue(response.getIds().containsAll(expectedIds));
    }

    @Test
    void givenMatchingGlobalFilter_whenGetEntryIds_thenReturnOnlyMatchingIds() throws Exception {
        ApplicationList matchingList = createAndSaveList(Status.OPEN);
        ApplicationListEntry matchingEntry = createEntry(matchingList);
        matchingEntry.setAccountNumber("GLOB-ID-MATCH");
        matchingEntry = persistance.save(matchingEntry);

        ApplicationList nonMatchingList = createAndSaveList(Status.OPEN);
        ApplicationListEntry nonMatchingEntry = createEntry(nonMatchingList);
        nonMatchingEntry.setAccountNumber("GLOB-ID-OTHER");
        nonMatchingEntry = persistance.save(nonMatchingEntry);

        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setAccountReference("GLOB-ID-MATCH");

        EntryIdsDto response = executeGlobalEntryIdsSearch(createAdminToken(), filter);

        Assertions.assertEquals(List.of(matchingEntry.getUuid()), response.getIds());
        assertThat(response.getIds()).doesNotContain(nonMatchingEntry.getUuid());
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

        EntryIdsDto idsResponse = executeGlobalEntryIdsSearch(createAdminToken(), filter);
        assertThat(idsResponse.getIds()).contains(matchingEntry.getUuid());
    }

    @Test
    void givenNoGlobalMatches_whenGetEntryIds_thenReturnEmptyList() throws Exception {
        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setAccountReference("NO-GLOB-MATCH");

        EntryIdsDto response = executeGlobalEntryIdsSearch(createAdminToken(), filter);

        Assertions.assertNotNull(response.getIds());
        assertThat(response.getIds()).isEmpty();
    }

    @Test
    void givenMoreThanOnePageOfGlobalMatches_whenGetEntryIds_thenReturnAllIds() throws Exception {
        List<UUID> expectedIds = new ArrayList<>();
        ApplicationCode applicationCode = createApplicationCode("GLOBIDS", true);

        for (int i = 1; i <= 12; i++) {
            ApplicationList list = createAndSaveList(Status.OPEN);
            ApplicationListEntry entry = createEntry(list);
            entry.setApplicationCode(applicationCode);
            entry.setAccountNumber("GLOB-MULTI");
            entry = persistance.save(entry);
            applicationCode =
                    createApplicationCodeCopy(
                            applicationCodeRepository
                                    .findById(applicationCode.getId())
                                    .orElseThrow());
            expectedIds.add(entry.getUuid());
        }

        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setAccountReference("GLOB-MULTI");

        EntryPage pagedResponse = executeSearch(createAdminToken(), filter, 10);
        EntryIdsDto response = executeGlobalEntryIdsSearch(createAdminToken(), filter);

        Assertions.assertEquals(10, pagedResponse.getContent().size());
        Assertions.assertEquals(12, pagedResponse.getTotalElements());
        Assertions.assertEquals(expectedIds.size(), response.getIds().size());
        Assertions.assertTrue(response.getIds().containsAll(expectedIds));
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

    private EntryIdsDto executeGlobalEntryIdsSearch(
            TokenGenerator tokenGenerator, EntryGetFilterDto filterDto) throws Exception {
        Response response =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/ids"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> {
                            if (filterDto == null) {
                                return rs;
                            }
                            if (filterDto.getDate() != null) {
                                rs = rs.queryParam("date", filterDto.getDate());
                            }
                            if (filterDto.getCourtCode() != null) {
                                rs = rs.queryParam("courtCode", filterDto.getCourtCode());
                            }
                            if (filterDto.getOtherLocationDescription() != null) {
                                rs =
                                        rs.queryParam(
                                                "otherLocationDescription",
                                                filterDto.getOtherLocationDescription());
                            }
                            if (filterDto.getCjaCode() != null) {
                                rs = rs.queryParam("cjaCode", filterDto.getCjaCode());
                            }
                            if (filterDto.getApplicantOrganisation() != null) {
                                rs =
                                        rs.queryParam(
                                                "applicantOrganisation",
                                                filterDto.getApplicantOrganisation());
                            }
                            if (filterDto.getApplicantSurname() != null) {
                                rs =
                                        rs.queryParam(
                                                "applicantSurname",
                                                filterDto.getApplicantSurname());
                            }
                            if (filterDto.getStandardApplicantCode() != null) {
                                rs =
                                        rs.queryParam(
                                                "standardApplicantCode",
                                                filterDto.getStandardApplicantCode());
                            }
                            if (filterDto.getStatus() != null) {
                                rs = rs.queryParam("status", filterDto.getStatus());
                            }
                            if (filterDto.getRespondentOrganisation() != null) {
                                rs =
                                        rs.queryParam(
                                                "respondentOrganisation",
                                                filterDto.getRespondentOrganisation());
                            }
                            if (filterDto.getRespondentSurname() != null) {
                                rs =
                                        rs.queryParam(
                                                "respondentSurname",
                                                filterDto.getRespondentSurname());
                            }
                            if (filterDto.getRespondentPostcode() != null) {
                                rs =
                                        rs.queryParam(
                                                "respondentPostcode",
                                                filterDto.getRespondentPostcode());
                            }
                            if (filterDto.getAccountReference() != null) {
                                rs =
                                        rs.queryParam(
                                                "accountReference",
                                                filterDto.getAccountReference());
                            }
                            return rs;
                        });

        response.then().statusCode(200);
        return response.as(EntryIdsDto.class);
    }

    private ApplicationListEntry createClosedStandardPersonApplicantEntry(
            String applicantFirstName, String applicantLastName, String accountReference) {
        StandardApplicant applicant = new StandardApplicant();
        applicant.setId(nextStandardApplicantId());
        applicant.setApplicantCode("SA" + accountReference);
        applicant.setApplicantStartDate(TEST_DATE);
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

    private EntryIdsDto executeListEntryIdsSearch(
            TokenGenerator tokenGenerator, UUID listId, EntryApplicationListGetFilterDto filterDto)
            throws Exception {
        Response response =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/ids"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> {
                            if (filterDto == null) {
                                return rs;
                            }
                            if (filterDto.getApplicantName() != null) {
                                rs = rs.queryParam("applicantName", filterDto.getApplicantName());
                            }
                            if (filterDto.getRespondentName() != null) {
                                rs = rs.queryParam("respondentName", filterDto.getRespondentName());
                            }
                            if (filterDto.getRespondentPostcode() != null) {
                                rs =
                                        rs.queryParam(
                                                "respondentPostcode",
                                                filterDto.getRespondentPostcode());
                            }
                            if (filterDto.getAccountReference() != null) {
                                rs =
                                        rs.queryParam(
                                                "accountReference",
                                                filterDto.getAccountReference());
                            }
                            if (filterDto.getApplicationTitle() != null) {
                                rs =
                                        rs.queryParam(
                                                "applicationTitle",
                                                filterDto.getApplicationTitle());
                            }
                            if (filterDto.getFeeRequired() != null) {
                                rs = rs.queryParam("feeRequired", filterDto.getFeeRequired());
                            }
                            if (filterDto.getResulted() != null) {
                                rs = rs.queryParam("resulted", filterDto.getResulted());
                            }
                            if (filterDto.getSequenceNumber() != null) {
                                rs = rs.queryParam("sequenceNumber", filterDto.getSequenceNumber());
                            }
                            return rs;
                        });

        response.then().statusCode(200);
        return response.as(EntryIdsDto.class);
    }
}
