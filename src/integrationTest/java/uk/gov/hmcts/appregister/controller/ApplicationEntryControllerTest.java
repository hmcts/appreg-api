package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;

public class ApplicationEntryControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "application-list-entries";

    private static final String CREATE_ENTRY_CONTEXT = "application-lists";

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    @Value("${spring.data.web.pageable.default-page-size}")
    private Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    private Integer maxPageSize;

    // The total app codes inserted by flyway scripts
    private static final int TOTAL_APP_ENTRY_COUNT = 10;

    // The deleted list that has been inserted by the flyway scripts
    private static final long DELETED_LIST_PK = 12;

    @Autowired private TransactionalUnitOfWork unitOfWork;

    @Autowired private ApplicationListRepository applicationListRepository;

    @Test
    public void testGetApplicationEntriesSearch() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, TOTAL_APP_ENTRY_COUNT);
        assertEquals(10, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().get(0);
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getName())
                .isEqualTo("Sarah Johnson");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("12 The Avenue");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getEmail())
                .isEqualTo("s.johnson@example.com");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getPostcode())
                .isEqualTo("XY9 8ZZ");

        assertThat(entryGetSummaryDto.getApplicationTitle())
                .isEqualTo("Appeal by Case Stated (Civil)");
        assertThat(entryGetSummaryDto.getLegislation())
                .isEqualTo("Section 111 Magistrates' Courts Act 1980");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getIsResulted()).isFalse();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        entryGetSummaryDto = page.getContent().get(4);
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getName())
                .isEqualTo("Legal Aid Board");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("100 Legal Street");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getEmail())
                .isEqualTo("info@legalaid.example.com");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getPostcode())
                .isEqualTo("BA15 1LA");

        assertThat(entryGetSummaryDto.getApplicationTitle())
                .isEqualTo("Request for Certificate of Refusal to State a Case (Civil)");
        assertThat(entryGetSummaryDto.getLegislation())
                .isEqualTo("Section 111 Magistrates' Courts Act 1980");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getIsResulted()).isFalse();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
    }

    @Test
    public void testGetApplicationEntriesSearchWithAllDetails() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setDate(LocalDate.parse("2024-04-21"));
        filterDto.setApplicantSurname("Turner");
        filterDto.setAccountReference("232323232");
        filterDto.setStatus(ApplicationListStatus.OPEN);
        filterDto.setCjaCode("CJ");
        filterDto.setCourtCode("RCJ001");
        filterDto.setOtherLocationDescription("other");
        filterDto.setRespondentOrganisation("Sarah Johnson");
        filterDto.setRespondentPostcode("XY9 8ZZ");
        filterDto.setStandardApplicantCode("APP002");

        // test the functionality
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

        // assert the response
        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        assertEquals(1, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().get(0);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
                .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSecondForename())
                .isEqualTo("Francis");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getThirdForename())
                .isEqualTo("Michael");

        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
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
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("12 The Avenue");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getEmail())
                .isEqualTo("s.johnson@example.com");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getPostcode())
                .isEqualTo("XY9 8ZZ");

        assertThat(entryGetSummaryDto.getApplicationTitle()).isEqualTo("Copy documents");
        assertThat(entryGetSummaryDto.getLegislation()).isEqualTo("");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isTrue();
        assertThat(entryGetSummaryDto.getIsResulted()).isTrue();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
    }

    @Test
    public void testGetApplicationEntriesSearchWithPartialAllDetails() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setDate(LocalDate.parse("2024-04-21"));
        filterDto.setApplicantSurname("rn");
        filterDto.setAccountReference("323232");
        filterDto.setStatus(ApplicationListStatus.OPEN);
        filterDto.setCjaCode("CJ");
        filterDto.setCourtCode("RCJ001");
        filterDto.setOtherLocationDescription("her");
        filterDto.setRespondentOrganisation("ah Johnson");
        filterDto.setRespondentPostcode("XY9 8ZZ");
        filterDto.setStandardApplicantCode("APP0");

        // test the functionality
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

        // assert the response
        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        assertEquals(1, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().get(0);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
                .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSecondForename())
                .isEqualTo("Francis");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getThirdForename())
                .isEqualTo("Michael");

        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
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
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("12 The Avenue");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getEmail())
                .isEqualTo("s.johnson@example.com");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getPostcode())
                .isEqualTo("XY9 8ZZ");

        assertThat(entryGetSummaryDto.getApplicationTitle()).isEqualTo("Copy documents");
        assertThat(entryGetSummaryDto.getLegislation()).isEqualTo("");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isTrue();
        assertThat(entryGetSummaryDto.getIsResulted()).isTrue();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
    }

    @Test
    public void testGetApplicationEntriesSearchWithAllSortKeys() throws Exception {
        for (ApplicationEntrySortFieldEnum applicationEntrySortFieldEnum :
                ApplicationEntrySortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(applicationEntrySortFieldEnum.getApiValue() + "," + "desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());
            responseSpec.then().statusCode(200);
        }

        Assertions.assertTrue(ApplicationEntrySortFieldEnum.values().length > 0);
    }

    @Test
    public void testGetApplicationEntriesSearchWithSort() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(ApplicationEntrySortFieldEnum.CODE.getApiValue() + "," + "desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, TOTAL_APP_ENTRY_COUNT);
        assertEquals(10, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().get(0);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
                .isEqualTo("Smith");
        assertThat(entryGetSummaryDto.getRespondent().getOrganisation().getName())
                .isEqualTo("Jack Turner");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("1 Market Street");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getEmail())
                .isEqualTo("john.smith@example.com");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getPostcode())
                .isEqualTo("AB11 2CD");

        assertThat(entryGetSummaryDto.getApplicationTitle())
                .isEqualTo("Certificate of Satisfaction");
        assertThat(entryGetSummaryDto.getLegislation()).isEqualTo("");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getIsResulted()).isFalse();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        entryGetSummaryDto = page.getContent().get(4);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
                .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSecondForename())
                .isEqualTo("Francis");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getThirdForename())
                .isEqualTo("Michael");

        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
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
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("12 The Avenue");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getEmail())
                .isEqualTo("s.johnson@example.com");
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getOrganisation()
                                .getContactDetails()
                                .getPostcode())
                .isEqualTo("XY9 8ZZ");

        assertThat(entryGetSummaryDto.getApplicationTitle()).isEqualTo("Copy documents");
        assertThat(entryGetSummaryDto.getLegislation()).isEqualTo("");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isTrue();
        assertThat(entryGetSummaryDto.getIsResulted()).isTrue();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationEntriesWithPageNumberBeyondResultBoundary_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 1;
        int pageNumber = 200;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 10, 10);
        Assertions.assertNull(page.getContent());
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationEntriesWithPagingInvalidSortQuery_thenReturn400()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("invalid-sort"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        // assert the response
        responseSpec.then().statusCode(400);
    }

    // NOTE: Spring is more forgiving in this scenario and defaults the page number to
    // 0 and returns a 200. Our implementation
    // returns a 500
    @Test
    public void
            givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageNumber_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = -1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        // assert the response
        responseSpec.then().statusCode(500);
    }

    // NOTE: Spring defaults the page size to the max size if we try and increase it beyond. This
    // does not behave
    // accordingly
    @Test
    public void
            givenValidRequest_whenGetApplicationEntriesWithPagingInvalidPageSizeBeyondDefault_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = maxPageSize + 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(500);
    }

    @Test
    public void testCreateListEntryWithAllData() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        // assert the response
        responseSpecCreate.then().statusCode(200);

        EntryGetDetailDto createdDto = responseSpecCreate.as(EntryGetDetailDto.class);

        // validate the response
        validateEntryCreationResponse(
                entryCreateDto, createdDto, List.of("Premises Address", "Premises Date"));

        // Now filter on the entry with the unique surname and assert we get a record back
        Response responseFindEntrySpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationEntryFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(surnameToLookup),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseFindEntrySpec.then().statusCode(200);

        EntryPage page = responseFindEntrySpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(createdDto.getId(), page.getContent().get(0).getId());

        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(10, true);

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "id",
                        "",
                        null,
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        "",
                        null,
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getEventName()));
    }

    @Test
    public void testApplicantMutualExclusiveWithAllApplicants() throws Exception {
        // setup the payload
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        List<Official> officials = Instancio.ofList(Official.class).size(4).create();
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(officials);

        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");

        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");
        entryCreateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA1 1AA");

        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail("RESPONDENT@TEST.COM");

        entryCreateDto.setNumberOfRespondents(10);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode("APP001");
        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of("test wording", LocalDate.now().toString()));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantMutualExclusiveWithTwoApplicants() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();

        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");

        entryCreateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");
        entryCreateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA1 1AA");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantMutualExclusiveWithAllRespondent() throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();

        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");
        entryCreateDto.getApplicant().setOrganisation(null);

        entryCreateDto.getRespondent().setOrganisation(Instancio.create(Organisation.class));
        entryCreateDto
                .getRespondent()
                .getOrganisation()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");
        entryCreateDto.getRespondent().getOrganisation().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail("RESPONDENT@TEST.COM");

        entryCreateDto.setNumberOfRespondents(10);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);
        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of("test wording", LocalDate.now().toString()));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantNoApplicationList() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + UUID.randomUUID() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(404);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantNoCorrectState() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getClosedApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantDeletedList() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getDeletedIdApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantNoCodeExists() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setApplicationCode("INVALID_CODE");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(404);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICANT_CODE_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantFeeRequired() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.getFeeStatuses().clear();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.FEE_REQUIRED.getCode().getType().get(), problemDetail.getType());
    }

    @Test
    public void testApplicantRespondentRequired() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantRespondentNotRequired() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setRespondent(null);

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testApplicantRespondentBulkNotAllowed() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setApplicationCode("AD99001");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.NOT_RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testWordingTemplateIncorrectFields() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(List.of("only one field", "extra field", "too many"));

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testWordingLengthFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(
                List.of("only one field that exceeds length", "extra field"));

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Address=only one field that exceeds length",
                problemDetail.getDetail().trim());
    }

    @Test
    public void testWordingDataTypeFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(List.of("value", "extra field not a date"));

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Date=extra field not a date", problemDetail.getDetail().trim());
    }

    /**
     * gets the correct payload to make a successful create entry.
     *
     * @return The created payload
     */
    private EntryCreateDto getCorrectCreateEntryDto() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        List<Official> officials = Instancio.ofList(Official.class).size(4).create();
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setOfficials(officials);

        entryCreateDto.getApplicant().setOrganisation(null);
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");

        entryCreateDto.getRespondent().setOrganisation(null);
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail("RESPONDENT@TEST.COM");

        entryCreateDto.setNumberOfRespondents(10);
        entryCreateDto.setNumberOfRespondents(null);
        entryCreateDto.setApplicationCode("MS99007");
        entryCreateDto.setStandardApplicantCode(null);
        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of("test wording", LocalDate.now().toString()));
        return entryCreateDto;
    }

    /**
     * validates the response based on the creation payload.
     *
     * @param entryCreateDto The creation payload
     * @param response The response to validate
     * @param expectedWordingFields The expected wording fields
     */
    private void validateEntryCreationResponse(
            EntryCreateDto entryCreateDto,
            EntryGetDetailDto response,
            List<String> expectedWordingFields) {

        if (entryCreateDto.getApplicant() != null) {
            Assertions.assertEquals(entryCreateDto.getApplicant(), response.getApplicant());
        } else if (entryCreateDto.getStandardApplicantCode() != null) {
            Assertions.assertNotNull(response.getStandardApplicantCode());
        }

        if (entryCreateDto.getRespondent() != null) {
            Assertions.assertEquals(entryCreateDto.getRespondent(), response.getRespondent());
        }

        // validate the response fields
        Assertions.assertEquals(entryCreateDto.getCaseReference(), response.getCaseReference());
        Assertions.assertEquals(entryCreateDto.getNotes(), response.getNotes());
        Assertions.assertEquals(entryCreateDto.getAccountNumber(), response.getAccountNumber());
        Assertions.assertEquals(expectedWordingFields, response.getWordingFields());
        Assertions.assertNotNull(response.getListId());
        Assertions.assertNotNull(response.getId());
        Assertions.assertEquals(
                entryCreateDto.getNumberOfRespondents(), response.getNumberOfRespondents());
        Assertions.assertEquals(entryCreateDto.getLodgementDate(), response.getLodgementDate());

        Assertions.assertEquals(entryCreateDto.getHasOffsiteFee(), response.getHasOffsiteFee());

        // ensure the response fees align
        for (int i = 0; i < response.getFeeStatuses().size(); i++) {
            Assertions.assertEquals(
                    entryCreateDto.getFeeStatuses().get(i).getPaymentReference(),
                    response.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    entryCreateDto.getFeeStatuses().get(i).getStatusDate(),
                    response.getFeeStatuses().get(i).getStatusDate());
            Assertions.assertEquals(
                    entryCreateDto.getFeeStatuses().get(i).getPaymentStatus(),
                    response.getFeeStatuses().get(i).getPaymentStatus());
        }

        // ensure the response fees align
        for (int i = 0; i < response.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getType(),
                    response.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getSurname(),
                    response.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getTitle(),
                    response.getOfficials().get(i).getTitle());
            Assertions.assertEquals(
                    entryCreateDto.getOfficials().get(i).getForename(),
                    response.getOfficials().get(i).getForename());
        }
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT
                                                + "/"
                                                + UUID.randomUUID()
                                                + "/entries"))
                        .method(HttpMethod.POST)
                        .payload(getCorrectCreateEntryDto())
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private UUID getOpenApplicationListId() {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository.findAll().getFirst();
                    return applicationList.getUuid();
                });
    }

    private UUID getClosedApplicationListId() {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList = applicationListRepository.findAll().get(2);
                    return applicationList.getUuid();
                });
    }

    private UUID getDeletedIdApplicationListId() {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository.findById(DELETED_LIST_PK).get();
                    return applicationList.getUuid();
                });
    }

    record ApplicationEntryFilter(
            Optional<LocalDate> date,
            Optional<String> courtCode,
            Optional<String> otherLocationDescription,
            Optional<String> cjaCode,
            Optional<String> applicantOrganisation,
            Optional<String> applicantSurname,
            Optional<String> status,
            Optional<String> respondentOrganisation,
            Optional<String> respondentSurname,
            Optional<String> respondentPostcode,
            Optional<String> accountReference,
            Optional<String> standardApplicantCode)
            implements UnaryOperator<RequestSpecification> {

        @Override
        public io.restassured.specification.RequestSpecification apply(
                io.restassured.specification.RequestSpecification rs) {
            if (date.isPresent()) {
                rs = rs.queryParam("date", date.get().toString());
            }

            if (otherLocationDescription.isPresent()) {
                rs = rs.queryParam("otherLocationDescription", otherLocationDescription.get());
            }

            if (cjaCode.isPresent()) {
                rs = rs.queryParam("cjaCode", cjaCode.get());
            }

            if (courtCode.isPresent()) {
                rs = rs.queryParam("courtCode", courtCode.get());
            }

            if (applicantOrganisation.isPresent()) {
                rs = rs.queryParam("applicantOrganisation", applicantOrganisation.get());
            }

            if (applicantSurname.isPresent()) {
                rs = rs.queryParam("applicantSurname", applicantSurname.get());
            }

            if (status.isPresent()) {
                rs = rs.queryParam("status", status.get());
            }

            if (respondentOrganisation.isPresent()) {
                rs = rs.queryParam("respondentOrganisation", respondentOrganisation.get());
            }

            if (respondentSurname.isPresent()) {
                rs = rs.queryParam("respondentSurname", respondentSurname.get());
            }

            if (respondentPostcode.isPresent()) {
                rs = rs.queryParam("respondentPostcode", respondentPostcode.get());
            }

            if (accountReference.isPresent()) {
                rs = rs.queryParam("accountReference", accountReference.get());
            }

            if (standardApplicantCode.isPresent()) {
                rs = rs.queryParam("standardApplicantCode", standardApplicantCode.get());
            }

            return rs;
        }
    }
}
