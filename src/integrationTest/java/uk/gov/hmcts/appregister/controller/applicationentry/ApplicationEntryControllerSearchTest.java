package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.appregister.common.security.RoleEnum.ADMIN;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
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

public class ApplicationEntryControllerSearchTest extends AbstractApplicationEntryCrudTest {

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
        Assertions.assertFalse(response.getIds().contains(nonMatchingEntry.getUuid()));
    }

    @Test
    void givenNoMatches_whenGetApplicationListEntryIds_thenReturnEmptyList() throws Exception {
        UUID listId = getOpenApplicationListId();

        EntryApplicationListGetFilterDto filter = new EntryApplicationListGetFilterDto();
        filter.setApplicantName("definitely-no-match-for-entry-ids");

        EntryIdsDto response = executeListEntryIdsSearch(createAdminToken(), listId, filter);

        Assertions.assertNotNull(response.getIds());
        Assertions.assertTrue(response.getIds().isEmpty());
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

    @StabilityTest
    public void testGetApplicationEntriesSearch() throws Exception {

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
    public void testGetApplicationEntriesSearchWithAllDetails() throws Exception {

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
                        "surname",
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
                        "surname",
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
    public void testGetApplicationEntriesSearchWithPartialAllDetails() throws Exception {
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
                        "surname",
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
                        "surname",
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
    public void
            givenApplicationEntryListSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
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
    public void
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
        Assertions.assertNull(page.getContent());

        dataAuditAssertionsForNoFilter();
    }

    @StabilityTest
    public void
            givenValidRequest_whenGetApplicationEntriesWithPagingInvalidSortQuery_thenReturn400()
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
    public void
            givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageNumber_thenReturn400()
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

    @StabilityTest
    public void
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
    public void givenValidRequest_whenMultipleSortsArePresent_thenReturn400() throws Exception {
        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize),
                        Optional.of(0),
                        List.of(
                                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                                ApplicationEntrySortFieldEnum.LOCATION.getApiValue()),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenInvalidRespondentPostcodeFilter_whenGetApplicationEntries_thenReturn400()
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
    public void
            givenPartialRespondentPostcodeMatch_whenGetApplicationEntries_thenReturnMatchingEntry()
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
    public void givenPartialRespondentPostcodeMiss_whenGetApplicationEntries_thenReturnEmptyPage()
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

        Assertions.assertNull(page.getContent());
    }

    @StabilityTest
    public void givenValidRequest_whenSortAccountNumber_thenReturn200() throws Exception {
        // set up the data
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        ApplicationListEntry applicationListEntry = createEntry(applicationList);
        applicationListEntry.setAccountNumber("z - a account number");
        persistance.save(applicationListEntry);

        ApplicationListEntry applicationListEntry1 = createEntry(applicationList);
        applicationListEntry1.setAccountNumber("z - c account number");
        persistance.save(applicationListEntry1);

        ApplicationListEntry applicationListEntry2 = createEntry(applicationList);
        applicationListEntry2.setAccountNumber("z - b account number");
        persistance.save(applicationListEntry2);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 5;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(
                                SortableField.getSortStringForDesc(
                                        ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE)),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        // make sure the order response marries with the request data
        Assertions.assertEquals(1, page.getSort().getOrders().size());
        Assertions.assertEquals(
                SortOrdersInner.DirectionEnum.DESC,
                page.getSort().getOrders().get(0).getDirection());

        // make sure we only return defaulted externalised api sort data
        Assertions.assertEquals(
                ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE.getApiValue(),
                page.getSort().getOrders().get(0).getProperty());

        // make sure the order is correct for the account number sort
        Assertions.assertEquals(applicationListEntry1.getUuid(), page.getContent().get(0).getId());
        Assertions.assertEquals(applicationListEntry2.getUuid(), page.getContent().get(1).getId());
        Assertions.assertEquals(applicationListEntry.getUuid(), page.getContent().get(2).getId());

        applicationListEntry = createEntry(applicationList);
        applicationListEntry.setAccountNumber("111111 - z");
        persistance.save(applicationListEntry);

        applicationListEntry1 = createEntry(applicationList);
        applicationListEntry1.setAccountNumber("111111 - c");
        persistance.save(applicationListEntry1);

        applicationListEntry2 = createEntry(applicationList);
        applicationListEntry2.setAccountNumber("111111 - b");
        persistance.save(applicationListEntry2);

        // execute the functionality with the opposite sort direction
        responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(
                                SortableField.getSortStringForAsc(
                                        ApplicationEntrySortFieldEnum.ACCOUNT_REFERENCE)),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        page = responseSpec.as(EntryPage.class);

        // make sure the order is correct for the account number sort
        Assertions.assertEquals(applicationListEntry2.getUuid(), page.getContent().get(0).getId());
        Assertions.assertEquals(applicationListEntry1.getUuid(), page.getContent().get(1).getId());
        Assertions.assertEquals(applicationListEntry.getUuid(), page.getContent().get(2).getId());
    }

    @Test
    @StabilityTest
    public void testGetApplicationEntriesSearchReturnsAllResultCodes() throws Exception {
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

        assertThat(entry.getApplicant().getPerson().getName().getSurname()).isEqualTo("Turner");
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
                        "surname",
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
