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
import org.apache.commons.lang3.RandomStringUtils;
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
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
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
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationEntryControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "application-list-entries";

    private static final String CREATE_ENTRY_CONTEXT = "application-lists";

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    @Value("${spring.data.web.pageable.default-page-size}")
    private Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    private Integer maxPageSize;

    // The total app entries inserted by flyway scripts
    private static final int TOTAL_APP_ENTRY_COUNT = 11;

    // The deleted list that has been inserted by the flyway scripts
    private static final long DELETED_LIST_PK = 12;

    @Autowired private TransactionalUnitOfWork unitOfWork;

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @StabilityTest
    public void testGetApplicationEntriesSearch() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(20),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 20, 0, 1, TOTAL_APP_ENTRY_COUNT);

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

    @StabilityTest
    public void testGetApplicationEntriesSearchWithAllDetails() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

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

    @StabilityTest
    public void testGetApplicationEntriesSearchWithPartialAllDetails() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

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

    @StabilityTest
    public void
            givenApplicationEntryListSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
                    throws Exception {
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

            EntryPage page = responseSpec.as(EntryPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());
            Assertions.assertEquals(
                    applicationEntrySortFieldEnum.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());
        }

        Assertions.assertTrue(ApplicationEntrySortFieldEnum.values().length > 0);
    }

    @StabilityTest
    public void testGetApplicationEntriesSearchWithSort() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(
                                ApplicationEntrySortFieldEnum.LEGISLATION.getApiValue()
                                        + ","
                                        + "desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        // PagingAssertionUtil.assertPageDetails(page, 10, 0, 2, TOTAL_APP_ENTRY_COUNT);
        assertEquals(10, page.getContent().size());

        EntryGetSummaryDto entryGetSummaryDto = page.getContent().get(0);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
                .isEqualTo("Jane");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
                .isEqualTo("Doe");
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

        entryGetSummaryDto = page.getContent().get(4);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstForename())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getSurname())
                .isEqualTo("Turner");

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

        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);
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
                .isEqualTo("Issue of liability order summons -council tax (bulk)");
        assertThat(entryGetSummaryDto.getLegislation())
                .isEqualTo("Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getIsResulted()).isFalse();
    }

    @StabilityTest
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
        PagingAssertionUtil.assertPageDetails(
                page, pageSize, pageNumber, TOTAL_APP_ENTRY_COUNT, TOTAL_APP_ENTRY_COUNT);
        Assertions.assertNull(page.getContent());
    }

    @StabilityTest
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
    @StabilityTest
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
        responseSpec.then().statusCode(400);
    }

    // NOTE: Spring defaults the page size to the max size if we try and increase it beyond. This
    // does not behave
    // accordingly
    @StabilityTest
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
        responseSpec.then().statusCode(400);
    }

    @Test
    public void givenValidRequest_whenCreateListEntry_thenReturn201() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        // arrange - token + create entry
        TokenGenerator tokenGenerator = createAdminToken();

        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        // assert we have a location header
        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto,
                List.of("Premises Address", "Premises Date"));

        // Now filter on the entry with the unique surname and assert we get a record back
        EntryPage page = findEntriesBySurname(tokenGenerator, surnameToLookup, 10, 0);

        // assert the page and id match
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(
                createdDto.getDetailDto().getId(), page.getContent().get(0).getId());

        // difference log / audit assertions
        differenceLogAsserter.assertNoErrors();
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
    public void
            givenAnInvalidCreateEntryRequest_whenCreateEntryWithApplicantApplicantMutualExclusiveInvalid_400IsReturned()
                    throws Exception {
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

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

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
    public void
            givenAnInvalidCreateEntryRequest_whenCreateEntryWithAppicantApplicantMutualExclusiveInvalid2_400IsReturned()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

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
    public void
            givenAnInvalidCreateEntryRequest_whenCreateEntryWithRespondentMutualExclusiveInvalid_400IsReturned()
                    throws Exception {
        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

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

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

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
    public void givenAnInvalidCreateEntryRequest_whenApplicationListDoesNotexist_404IsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

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
    public void
            givenAnInvalidCreateEntryRequest_whenApplicationListIsNotInCorrectState_400IsReturned()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

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
        responseSpecCreate.then().statusCode(409);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenApplicationListIsNotInCorrectStateDeleted_400IsReturned()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

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
        responseSpecCreate.then().statusCode(409);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE
                        .getCode()
                        .getType()
                        .get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenApplicationCodeDoesNotExist_404IsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
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
                AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenFeeNotExist_404IsReturned() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
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
    public void givenAnInvalidCreateEntryRequest_whenRespondentNotExist_404IsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
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
    public void givenAnInvalidCreateEntryRequest_whenRespondentNotRequired_400IsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
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
    public void givenAnInvalidCreateEntryRequest_whenApplicantBulkNotAllowed_400IsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
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
                AppListEntryError.RESPONDENT_NOT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void
            givenAnInvalidCreateEntryRequest_whenWordingTemplateFieldsNotSufficient_400IsReturned()
                    throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field not a date");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution2.setKey("too many");
        substitution2.setValue("val");

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1, substitution2));

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
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenWordingLengthNotSufficient_400IsReturned()
            throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("only one field that exceeds length");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field");

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

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
                CommonAppError.WORDING_LENGTH_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Address=only one field that exceeds length",
                problemDetail.getDetail().trim());
    }

    @Test
    public void givenAnInvalidCreateEntryRequest_whenWordingDataTypeFailure_400IsReturned()
            throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field not a date");

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

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
                CommonAppError.WORDING_DATA_TYPE_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Date=extra field not a date", problemDetail.getDetail().trim());
    }

    @StabilityTest
    public void givenCreatedEntrySoftDeletedViaRepository_whenSearchingEntries_thenEntryIsExcluded()
            throws Exception {

        // arrange
        TokenGenerator tokenGenerator = createAdminToken();

        // create the entry with a unique surname
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String uniqueSurname = "DELTEST-" + UUID.randomUUID();
        SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, uniqueSurname);

        Assertions.assertNotNull(createdDto);
        Assertions.assertNotNull(
                createdDto.getDetailDto.getId(), "Created entry must contain an id");
        UUID createdUuid = createdDto.getDetailDto.getId();

        // Soft-delete the created entry
        int rowsUpdated =
                unitOfWork.inTransaction(
                        () -> applicationListEntryRepository.softDeleteByUuid(createdUuid));
        Assertions.assertEquals(
                1, rowsUpdated, "Expected exactly one application list entry to be soft-deleted");

        // Call the GET endpoint with a large page size to fetch all results (no filters)
        int pageSize = Math.max(defaultPageSize, 100);
        EntryPage page = findAllEntriesWithLargePage(tokenGenerator, pageSize, 0);

        // Assert the deleted entry is NOT present in the results
        boolean foundDeleted =
                page.getContent() != null
                        && page.getContent().stream()
                                .anyMatch(s -> s.getId() != null && s.getId().equals(createdUuid));

        Assertions.assertFalse(
                foundDeleted,
                "Entry that was soft-deleted via repository must NOT be returned by the search");
    }

    private EntryUpdateDto getCorrectUpdateDataDto() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        final EntryUpdateDto updateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();

        final List<Official> officials = Instancio.ofList(Official.class).size(4).create();

        updateDto.getApplicant().setPerson(null);
        updateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA1 12B");
        updateDto.getApplicant().getOrganisation().getContactDetails().setEmail("test@org.com");

        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        updateDto.getRespondent().getPerson().getContactDetails().setEmail("test@test.com");
        updateDto.getRespondent().setOrganisation(null);

        updateDto.setStandardApplicantCode(null);
        updateDto.setNumberOfRespondents(0);
        updateDto.setOfficials(officials);

        // use the applicant standard applicant
        updateDto.setNumberOfRespondents(null);
        updateDto.setApplicationCode("ZS99007");
        updateDto.setHasOffsiteFee(true);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        updateDto.setWordingFields(List.of(substitution, substitution1));
        return updateDto;
    }

    @Test
    public void givenASuccessfulUpdate_whenAllValueAreToBeUpdate_200Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        differenceLogAsserter.clearLogs();
        differenceLogAsserter.assertNoErrors();
        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecCreate.then().statusCode(201);
        responseSpecUpdate.then().statusCode(200);

        EntryGetDetailDto updatedDto = responseSpecUpdate.as(EntryGetDetailDto.class);
        EntryGetDetailDto createDDto = responseSpecCreate.as(EntryGetDetailDto.class);

        // validate the update response
        validateEntryUpdateResponse(
                entryUpdateDto,
                updatedDto,
                List.of("Premises Address", "Premises Date"),
                createDDto.getFeeStatuses());

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
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(
                                        updatedDto
                                                .getRespondent()
                                                .getPerson()
                                                .getName()
                                                .getSurname()),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseFindEntrySpec.then().statusCode(200);

        // assert that we returned a result
        EntryPage page = responseFindEntrySpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(updatedDto.getId(), page.getContent().get(0).getId());

        differenceLogAsserter.assertNoErrors();

        // assert the application entry update audit logs
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        "",
                        "1",
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        "",
                        null,
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.UPDATE_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "na_id",
                        null,
                        null,
                        AppListEntryAuditOperation.CREATE_APPLICANT.getType().name(),
                        AppListEntryAuditOperation.CREATE_APPLICANT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "na_id",
                        null,
                        null,
                        AppListEntryAuditOperation.DELETE_APPLICANT.getType().name(),
                        AppListEntryAuditOperation.DELETE_APPLICANT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "na_id",
                        null,
                        null,
                        AppListEntryAuditOperation.CREATE_RESPONDENT.getType().name(),
                        AppListEntryAuditOperation.CREATE_RESPONDENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.NAME_ADDRESS,
                        "na_id",
                        null,
                        null,
                        AppListEntryAuditOperation.CREATE_RESPONDENT.getType().name(),
                        AppListEntryAuditOperation.CREATE_RESPONDENT.getEventName()));

        Assertions.assertEquals(
                entryUpdateDto.getFeeStatuses().size(),
                differenceLogAsserter.assertDataAuditChange(
                        AuditLogAsserter.getDataAuditAssertion(
                                TableNames.APPLICATION_LISTS_FEE_STATUS,
                                "alefs_id",
                                null,
                                null,
                                AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY.getType().name(),
                                AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY
                                        .getEventName())));

        Assertions.assertEquals(
                entryUpdateDto.getOfficials().size(),
                differenceLogAsserter.assertDataAuditChange(
                        AuditLogAsserter.getDataAuditAssertion(
                                TableNames.APPLCATION_LISTS_ENTRY_OFFICIAL,
                                "aleo_id",
                                null,
                                null,
                                AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY.getType().name(),
                                AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY.getEventName())));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLCATION_LISTS_ENTRY_OFFICIAL,
                        "aleo_id",
                        null,
                        null,
                        AppListEntryAuditOperation.DELETE_OFFICIAL_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_OFFICIAL_ENTRY.getEventName()));

        Assertions.assertEquals(
                1,
                differenceLogAsserter.assertDataAuditChange(
                        AuditLogAsserter.getDataAuditAssertion(
                                TableNames.APPLCATION_LISTS_ENTRY_FEE_ID,
                                "ale_ale_id",
                                null,
                                null,
                                AppListEntryAuditOperation.CREATE_FEE_ENTRY.getType().name(),
                                AppListEntryAuditOperation.CREATE_FEE_ENTRY.getEventName())));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLCATION_LISTS_ENTRY_FEE_ID,
                        "ale_ale_id",
                        null,
                        null,
                        AppListEntryAuditOperation.DELETE_FEE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_FEE_ENTRY.getEventName()));
    }

    @Test
    public void givenAFailureUpdate_whenAnEntryToUpdateDoesntExist_404Returned() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecUpdate.then().statusCode(404);
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenAnEntryIsNotPartOfList_400Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();
        EntryGetDetailDto createdData = responseSpecCreate.as(EntryGetDetailDto.class);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        // test the functionality
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries/"
                                        + createdData.getId()),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        // assert the response
        responseSpecUpdate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenAnEtagFailingMatch_412Returned() throws Exception {
        Response responseSpecCreate = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        // update entry
        Response responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);

        responseSpecUpdate.then().statusCode(200);

        // update with an old etag
        entryUpdateDto = getCorrectUpdateDataDto();

        responseSpecUpdate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(responseSpecCreate),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto,
                        HeaderUtil.getETag(responseSpecCreate));

        // assert the response
        responseSpecUpdate.then().statusCode(412);
        ProblemDetail problemDetail = responseSpecUpdate.as(ProblemDetail.class);

        Assertions.assertEquals(
                CommonAppError.MATCH_ETAG_FAILURE.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenApplicantMutualExclusionFailure_400Returned()
            throws Exception {
        final Response entryResponse = createListEntryWithAllData();

        // create the token
        final TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.getApplicant().setPerson(null);
        entryUpdateDto.getApplicant().setOrganisation(null);

        entryUpdateDto.setStandardApplicantCode("MS99007");

        entryUpdateDto.getApplicant().setOrganisation(Instancio.create(Organisation.class));
        entryUpdateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");
        entryUpdateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA1 1AA");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
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
    public void
            givenAFailureUpdate_whenApplicantMutualExclusionFailureWithAllValuesSet_400Returned()
                    throws Exception {
        final Response entryResponse = createListEntryWithAllData();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.getRespondent().setOrganisation(Instancio.create(Organisation.class));
        entryUpdateDto
                .getRespondent()
                .getOrganisation()
                .getContactDetails()
                .setEmail("APPLICANT@TEST.COM");
        entryUpdateDto.getRespondent().getOrganisation().getContactDetails().setPostcode("AA1 1AA");
        entryUpdateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
        entryUpdateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail("RESPONDENT@TEST.COM");

        entryUpdateDto.setNumberOfRespondents(10);
        entryUpdateDto.setNumberOfRespondents(null);
        entryUpdateDto.setApplicationCode("MS99007");
        entryUpdateDto.setStandardApplicantCode(null);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        // fill the template with the two parameters
        entryUpdateDto.setWordingFields(List.of(substitution, substitution1));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
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
    public void givenAFailureUpdate_whenApplicantCodeDoesntExist_404Returned() throws Exception {
        Response entryResponse = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        // setup the payload
        entryUpdateDto.setApplicationCode("INVALID_CODE");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(404);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenApplicantFeeRequiredButNotSupplied_400Returned()
            throws Exception {
        Response entryResponse = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.getFeeStatuses().clear();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.FEE_REQUIRED.getCode().getType().get(), problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenRespondentRequiredButNotSupplied_400Returned()
            throws Exception {
        Response entryResponse = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setRespondent(null);

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenRespondentSuppliedButNotRequired_400Returned()
            throws Exception {
        Response entryResponse = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setRespondent(null);

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenBulkRespondentNotExpected_400Returned() throws Exception {
        Response entryResponse = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setApplicationCode("AD99001");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.RESPONDENT_NOT_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenWordingTemplateFieldsNotAcceptable_400Returned()
            throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field not a date");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution2.setKey("too many");
        substitution2.setValue("val");

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setWordingFields(List.of(substitution, substitution1, substitution2));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response entryResponse = createListEntryWithAllData();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void givenAFailureUpdate_whenWordingTemplateFieldsLengthNotAcceptable_400Returned()
            throws Exception {
        // setup the payload
        String stringExceedLength = RandomStringUtils.insecure().nextAlphanumeric(201);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue(stringExceedLength);

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setWordingFields(List.of(substitution, substitution1));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response entryResponse = createListEntryWithAllData();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Address=" + stringExceedLength, problemDetail.getDetail().trim());
    }

    @Test
    public void givenAFailureUpdate_whenWordingTemplateFieldsDataTypeNotAcceptable_400Returned()
            throws Exception {

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("value");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue("extra field not a date");

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();

        entryUpdateDto.setWordingFields(List.of(substitution, substitution1));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response entryResponse = createListEntryWithAllData();

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(400);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);

        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Premises Date=extra field not a date", problemDetail.getDetail().trim());
    }

    @Test
    public void givenAFailureUpdate_whenStandardApplicantNotFound_404Returned() throws Exception {
        Response entryResponse = createListEntryWithAllData();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryUpdateDto entryUpdateDto = getCorrectUpdateDataDto();
        entryUpdateDto.setApplicant(null);
        entryUpdateDto.setStandardApplicantCode("INVALID_CODE");

        // test the functionality
        Response responseSpecCreate =
                restAssuredClient.executePutRequest(
                        HeaderUtil.getLocation(entryResponse),
                        tokenGenerator.fetchTokenForRole(),
                        entryUpdateDto);
        responseSpecCreate.then().statusCode(404);
        ProblemDetail problemDetail = responseSpecCreate.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    /** Build a token generator with ADMIN role. */
    private TokenGenerator createAdminToken() {
        return getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
    }

    /**
     * creates a list entry with a respondent, applicant, fees, officials and wording fields.
     *
     * @return The url to the created entry
     */
    public Response createListEntryWithAllData() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // setup the payload
        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
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
        responseSpecCreate.then().statusCode(201);

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
        //        differenceLogAsserter.assertDiffCount(10, true);

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
                        null,
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

        return responseSpecCreate;
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
                        .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT
                                                + "/"
                                                + UUID.randomUUID()
                                                + "/entries/"
                                                + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(CreateEntryDtoUtil.getCorrectCreateEntryDto())
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

    /**
     * Creates an entry using provided DTO and unique surname (overwrites DTO surname). Asserts
     * creation status (201) and returns parsed EntryGetDetailDto.
     */
    private SuccessCreateEntryResponse createEntryWithUniqueSurname(
            TokenGenerator tokenGenerator, EntryCreateDto entryCreateDto, String uniqueSurname)
            throws Exception {

        // ensure DTO has the unique surname
        entryCreateDto.getApplicant().getPerson().getName().setSurname(uniqueSurname);

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
        responseSpecCreate.then().statusCode(201);

        EntryGetDetailDto createdDto = responseSpecCreate.as(EntryGetDetailDto.class);

        // validate the response
        validateEntryCreationResponse(
                entryCreateDto, createdDto, List.of("Premises Address", "Premises Date"));

        // assert creation
        responseSpecCreate.then().statusCode(201);
        Assertions.assertNotNull(HeaderUtil.getETag(responseSpecCreate));

        return new SuccessCreateEntryResponse(
                responseSpecCreate.as(EntryGetDetailDto.class), responseSpecCreate);
    }

    /** Finds entries by surname using the application entry filter and returns EntryPage. */
    private EntryPage findEntriesBySurname(
            TokenGenerator tokenGenerator, String surname, int size, int page) throws Exception {

        Response responseFindEntrySpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(size),
                        Optional.of(page),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationEntryFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(surname),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseFindEntrySpec.then().statusCode(200);
        return responseFindEntrySpec.as(EntryPage.class);
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

    /**
     * validates the response based on the update payload.
     *
     * @param entryUpdateDto The creation payload
     * @param response The response to validate
     * @param expectedWordingFields The expected wording fields
     * @param existingFees The existing fees before the update
     */
    private void validateEntryUpdateResponse(
            EntryUpdateDto entryUpdateDto,
            EntryGetDetailDto response,
            List<String> expectedWordingFields,
            List<FeeStatus> existingFees) {

        if (entryUpdateDto.getApplicant() != null) {
            Assertions.assertEquals(entryUpdateDto.getApplicant(), response.getApplicant());
        } else if (entryUpdateDto.getStandardApplicantCode() != null) {
            Assertions.assertNotNull(response.getStandardApplicantCode());
        }

        if (entryUpdateDto.getRespondent() != null) {
            Assertions.assertEquals(entryUpdateDto.getRespondent(), response.getRespondent());
        }

        // validate the response fields
        Assertions.assertEquals(entryUpdateDto.getCaseReference(), response.getCaseReference());
        Assertions.assertEquals(entryUpdateDto.getNotes(), response.getNotes());
        Assertions.assertEquals(entryUpdateDto.getAccountNumber(), response.getAccountNumber());
        Assertions.assertEquals(expectedWordingFields, response.getWordingFields());
        Assertions.assertNotNull(response.getListId());
        Assertions.assertNotNull(response.getId());
        Assertions.assertEquals(
                entryUpdateDto.getNumberOfRespondents(), response.getNumberOfRespondents());
        Assertions.assertEquals(entryUpdateDto.getLodgementDate(), response.getLodgementDate());

        Assertions.assertEquals(entryUpdateDto.getHasOffsiteFee(), response.getHasOffsiteFee());

        // ensure the existing feeds are present in the correct order
        for (int i = 0; i < existingFees.size(); i++) {
            Assertions.assertEquals(
                    existingFees.get(i).getPaymentReference(),
                    response.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    existingFees.get(i).getStatusDate(),
                    response.getFeeStatuses().get(i).getStatusDate());
            Assertions.assertEquals(
                    existingFees.get(i).getPaymentStatus(),
                    response.getFeeStatuses().get(i).getPaymentStatus());
        }

        // ensure the response fees align
        for (int i = existingFees.size(); i < response.getFeeStatuses().size(); i++) {
            Assertions.assertEquals(
                    entryUpdateDto
                            .getFeeStatuses()
                            .get(i - existingFees.size())
                            .getPaymentReference(),
                    response.getFeeStatuses().get(i).getPaymentReference());
            Assertions.assertEquals(
                    entryUpdateDto.getFeeStatuses().get(i - existingFees.size()).getStatusDate(),
                    response.getFeeStatuses().get(i).getStatusDate());
            Assertions.assertEquals(
                    entryUpdateDto.getFeeStatuses().get(i - existingFees.size()).getPaymentStatus(),
                    response.getFeeStatuses().get(i).getPaymentStatus());
        }

        // ensure the response fees align
        for (int i = 0; i < response.getOfficials().size(); i++) {
            Assertions.assertEquals(
                    entryUpdateDto.getOfficials().get(i).getType(),
                    response.getOfficials().get(i).getType());
            Assertions.assertEquals(
                    entryUpdateDto.getOfficials().get(i).getSurname(),
                    response.getOfficials().get(i).getSurname());
            Assertions.assertEquals(
                    entryUpdateDto.getOfficials().get(i).getTitle(),
                    response.getOfficials().get(i).getTitle());
            Assertions.assertEquals(
                    entryUpdateDto.getOfficials().get(i).getForename(),
                    response.getOfficials().get(i).getForename());
        }
    }

    /** Calls the GET paging endpoint without filters and returns EntryPage. */
    private EntryPage findAllEntriesWithLargePage(TokenGenerator tokenGenerator, int size, int page)
            throws Exception {

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(size),
                        Optional.of(page),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        return responseSpec.as(EntryPage.class);
    }

    record SuccessCreateEntryResponse(EntryGetDetailDto getDetailDto, Response response) {}
}
