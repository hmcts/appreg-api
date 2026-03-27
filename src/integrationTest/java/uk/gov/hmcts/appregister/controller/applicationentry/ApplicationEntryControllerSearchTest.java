package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.appregister.common.enumeration.YesOrNo.NO;
import static uk.gov.hmcts.appregister.common.security.RoleEnum.ADMIN;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationEntryControllerSearchTest extends AbstractApplicationEntryCrudTest {

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private AppListEntryResolutionRepository appListEntryResolutionRepository;

    @Autowired private ResolutionCodeRepository resolutionCodeRepository;

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

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
        ApplicationList list = new ApplicationList();
        list.setDate(LocalDate.now());
        list.setTime(LocalTime.of(9, 0));
        list.setStatus(Status.OPEN);
        list.setDescription("Test list description");
        list = persistance.save(list);

        ApplicationCode applicationCode = buildApplicationCode("APP002");
        applicationCode = persistance.save(applicationCode);

        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setApplicationList(list);
        entry.setApplicationCode(applicationCode);
        entry.setApplicationListEntryWording("Test entry wording");
        entry.setEntryRescheduled("N");
        entry.setSequenceNumber((short) 1);
        entry.setLodgementDate(LocalDate.now());
        entry.setCreatedUser("email");
        entry.setAccountNumber("RESULT-12345");
        entry.setCaseReference("CASE123");
        entry.setBulkUpload("N");
        entry.setRetryCount("0");
        entry.setNotes("Test notes");
        entry.setTcepStatus("NW");
        entry = persistance.save(entry);

        saveResolution(entry, "RC1");
        saveResolution(entry, "RC2");

        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setAccountReference("RESULT-12345");

        TokenGenerator tokenGenerator = createAdminToken();
        EntryPage page = executeSearch(tokenGenerator, filterDto, 20);

        assertThat(page.getContent()).isNotNull();
        assertEquals(1, page.getContent().size());

        EntryGetSummaryDto dto = page.getContent().getFirst();
        assertThat(dto.getIsResulted()).isTrue();
        assertEquals(2, dto.getResulted().size());

        Set<String> codes =
            dto.getResulted().stream()
                .map(ResultCodeGetSummaryDto::getResultCode)
                .collect(Collectors.toSet());

        assertEquals(Set.of("RC1", "RC2"), codes);
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

    private void saveResolution(ApplicationListEntry sourceEntry, String resultCode) {
        ApplicationListEntry persistedEntry =
            applicationListEntryRepository.findById(sourceEntry.getId()).orElseThrow();

        ApplicationCode persistedCode =
            applicationCodeRepository.findById(
                sourceEntry.getApplicationCode().getId()).orElseThrow();

        ApplicationList persistedList =
            applicationListRepository.findById(
                persistedEntry.getApplicationList().getId()).orElseThrow();

        ResolutionCode code = new ResolutionCode();
        code.setResultCode(resultCode);
        code.setTitle(resultCode + " title");
        code.setWording(resultCode + " wording");
        code.setLegislation("Test legislation");
        code.setStartDate(LocalDate.now());
        code.setChangedBy(1L);
        code.setChangedDate(OffsetDateTime.now());
        code = persistance.save(code);

        ApplicationCode applicationCodeCopy = getApplicationCode(persistedCode);

        ApplicationListEntry entryCopy =
            getApplicationListEntry(persistedEntry, persistedList, applicationCodeCopy);

        AppListEntryResolution entryResolution = new AppListEntryResolution();
        entryResolution.setApplicationList(entryCopy);
        entryResolution.setResolutionCode(code);
        entryResolution.setResolutionWording(resultCode + " wording");
        entryResolution.setResolutionOfficer("Test officer");

        persistance.save(entryResolution);
    }

    private static @NotNull ApplicationCode getApplicationCode(ApplicationCode persistedCode) {
        ApplicationCode applicationCodeCopy = new ApplicationCode();
        applicationCodeCopy.setId(persistedCode.getId());
        applicationCodeCopy.setVersion(persistedCode.getVersion());
        applicationCodeCopy.setCode(persistedCode.getCode());
        applicationCodeCopy.setTitle(persistedCode.getTitle());
        applicationCodeCopy.setWording(persistedCode.getWording());
        applicationCodeCopy.setLegislation(persistedCode.getLegislation());
        applicationCodeCopy.setFeeDue(persistedCode.getFeeDue());
        applicationCodeCopy.setRequiresRespondent(persistedCode.getRequiresRespondent());
        applicationCodeCopy.setBulkRespondentAllowed(persistedCode.getBulkRespondentAllowed());
        applicationCodeCopy.setStartDate(persistedCode.getStartDate());
        applicationCodeCopy.setChangedBy(persistedCode.getChangedBy());
        applicationCodeCopy.setChangedDate(persistedCode.getChangedDate());
        applicationCodeCopy.setCreatedUser(persistedCode.getCreatedUser());
        applicationCodeCopy.setApplicationListEntryList(null);
        return applicationCodeCopy;
    }

    private static @NotNull ApplicationListEntry getApplicationListEntry(
        ApplicationListEntry persistedEntry,
        ApplicationList persistedList,
        ApplicationCode applicationCodeCopy) {
        ApplicationListEntry entryCopy = new ApplicationListEntry();
        entryCopy.setId(persistedEntry.getId());
        entryCopy.setUuid(persistedEntry.getUuid());
        entryCopy.setVersion(persistedEntry.getVersion());
        entryCopy.setApplicationList(persistedList);
        entryCopy.setApplicationCode(applicationCodeCopy);
        entryCopy.setApplicationListEntryWording(persistedEntry.getApplicationListEntryWording());
        entryCopy.setEntryRescheduled(persistedEntry.getEntryRescheduled());
        entryCopy.setSequenceNumber(persistedEntry.getSequenceNumber());
        entryCopy.setLodgementDate(persistedEntry.getLodgementDate());
        entryCopy.setCreatedUser(persistedEntry.getCreatedUser());
        entryCopy.setAccountNumber(persistedEntry.getAccountNumber());
        entryCopy.setCaseReference(persistedEntry.getCaseReference());
        entryCopy.setBulkUpload(persistedEntry.getBulkUpload());
        entryCopy.setRetryCount(persistedEntry.getRetryCount());
        entryCopy.setTcepStatus(persistedEntry.getTcepStatus());
        entryCopy.setNotes(persistedEntry.getNotes());
        return entryCopy;
    }

    private ApplicationCode buildApplicationCode(String code) {
        ApplicationCode applicationCode = new ApplicationCode();
        applicationCode.setCode(code);
        applicationCode.setTitle("Test title");
        applicationCode.setWording("Test wording");
        applicationCode.setLegislation("Test legislation");
        applicationCode.setFeeDue(NO);
        applicationCode.setRequiresRespondent(NO);
        applicationCode.setBulkRespondentAllowed(NO);
        applicationCode.setStartDate(LocalDate.now());
        applicationCode.setChangedBy(1L);
        applicationCode.setChangedDate(OffsetDateTime.now());
        applicationCode.setCreatedUser("email");
        return applicationCode;
    }
}
