package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;

import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationEntryControllerSearchTest extends AbstractApplicationEntryCrudTest {

    @StabilityTest
    public void testGetApplicationEntriesSearch() throws Exception {
        var tokenGenerator = createAdminToken();

        Response responseSpec =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(20),
                Optional.of(0),
                List.of(),
                getLocalUrl(WEB_CONTEXT),
                tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 20, 0, 1, TOTAL_APP_ENTRY_COUNT);

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().getFirst();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getName())
            .isEqualTo("Sarah Johnson");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getContactDetails().getAddressLine1())
            .isEqualTo("12 The Avenue");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getContactDetails().getEmail())
            .isEqualTo("s.johnson@example.com");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getContactDetails().getPostcode())
            .isEqualTo("XY9 8ZZ");

        assertThat(entryGetSummaryDto.getApplicationTitle())
            .isEqualTo("Certified genuine copy document");
        assertThat(entryGetSummaryDto.getLegislation()).isEqualTo("");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getIsResulted()).isFalse();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        entryGetSummaryDto = page.getContent().get(4);
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getName())
            .isEqualTo("Legal Aid Board");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getContactDetails().getAddressLine1())
            .isEqualTo("100 Legal Street");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getContactDetails().getEmail())
            .isEqualTo("info@legalaid.example.com");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getContactDetails().getPostcode())
            .isEqualTo("BA15 1LA");

        assertThat(entryGetSummaryDto.getApplicationTitle())
            .isEqualTo("Request for Certificate of Refusal to State a Case (Civil)");
        assertThat(entryGetSummaryDto.getLegislation())
            .isEqualTo("Section 111 Magistrates' Courts Act 1980");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getIsResulted()).isFalse();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2025-04-21"));
        assertThat(entryGetSummaryDto.getListId()).isNotNull();
    }

    @StabilityTest
    public void testGetApplicationEntriesSearchWithAllDetails() throws Exception {
        var tokenGenerator = createAdminToken();

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

        Response responseSpec =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(10),
                Optional.of(0),
                List.of(),
                getLocalUrl(WEB_CONTEXT),
                tokenGenerator.fetchTokenForRole(),
                new ApplicationEntryFilter(
                    Optional.of(filterDto.getDate()),
                    Optional.of(filterDto.getCourtCode()),
                    Optional.empty(),
                    Optional.of(filterDto.getCjaCode()),
                    Optional.empty(),
                    Optional.of(filterDto.getApplicantSurname()),
                    Optional.of(filterDto.getStatus().toString()),
                    Optional.of(filterDto.getRespondentOrganisation()),
                    Optional.empty(),
                    Optional.of(filterDto.getRespondentPostcode()),
                    Optional.of(filterDto.getAccountReference()),
                    Optional.of(filterDto.getStandardApplicantCode())),
                new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(1, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().getFirst();
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
            .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
            .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSecondForename())
            .isEqualTo("Francis");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getThirdForename())
            .isEqualTo("Michael");

        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getAddressLine1())
            .isEqualTo("1 Market Street");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getEmail())
            .isEqualTo("john.smith@example.com");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getPostcode())
            .isEqualTo("AB11 2CD");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getPhone())
            .isEqualTo("01234567890");

        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getName())
            .isEqualTo("Sarah Johnson");

        assertThat(entryGetSummaryDto.getApplicationTitle()).isEqualTo("Copy documents");
        assertThat(entryGetSummaryDto.getLegislation()).isEqualTo("");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isTrue();
        assertThat(entryGetSummaryDto.getIsResulted()).isTrue();
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2024-04-21"));
        assertThat(entryGetSummaryDto.getListId()).isNotNull();
    }

    @StabilityTest
    public void testGetApplicationEntriesSearchWithPartialAllDetails() throws Exception {
        var tokenGenerator = createAdminToken();

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

        Response responseSpec =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(10),
                Optional.of(0),
                List.of(),
                getLocalUrl(WEB_CONTEXT),
                tokenGenerator.fetchTokenForRole(),
                new ApplicationEntryFilter(
                    Optional.of(filterDto.getDate()),
                    Optional.of(filterDto.getCourtCode()),
                    Optional.empty(),
                    Optional.of(filterDto.getCjaCode()),
                    Optional.empty(),
                    Optional.of(filterDto.getApplicantSurname()),
                    Optional.of(filterDto.getStatus().toString()),
                    Optional.of(filterDto.getRespondentOrganisation()),
                    Optional.empty(),
                    Optional.of(filterDto.getRespondentPostcode()),
                    Optional.of(filterDto.getAccountReference()),
                    Optional.of(filterDto.getStandardApplicantCode())),
                new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(1, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().getFirst();
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
            .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
            .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicationTitle()).isEqualTo("Copy documents");
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isTrue();
        assertThat(entryGetSummaryDto.getIsResulted()).isTrue();
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2024-04-21"));
    }

    @StabilityTest
    public void givenApplicationEntryListSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
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
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                SortOrdersInner.DirectionEnum.DESC,
                page.getSort().getOrders().getFirst().getDirection());
            Assertions.assertEquals(
                sortField.getApiValue(),
                page.getSort().getOrders().getFirst().getProperty());
        }

        Assertions.assertTrue(ApplicationEntrySortFieldEnum.values().length > 0);
    }

    @StabilityTest
    public void givenValidRequest_whenGetApplicationEntriesWithPageNumberBeyondResultBoundary_thenReturn200()
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
    }

    @StabilityTest
    public void givenValidRequest_whenGetApplicationEntriesWithPagingInvalidSortQuery_thenReturn400()
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
    public void givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageNumber_thenReturn200()
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
    public void givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageSizeBeyondDefault_thenReturn200()
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
}
