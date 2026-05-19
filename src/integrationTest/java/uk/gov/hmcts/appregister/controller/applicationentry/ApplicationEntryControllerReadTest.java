package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.val;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntryByListIdSortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
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

public class ApplicationEntryControllerReadTest extends AbstractApplicationEntryCrudTest {

    @Autowired private DataAuditRepository dataAuditRepository;

    @Test
    @StabilityTest
    public void testGetApplicationEntrySuccess() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + uuids[0] + "/entries/" + uuids[1]),
                        tokenGenerator.fetchTokenForRole());

        EntryGetDetailDto entryGetDetailDto = responseSpec.as(EntryGetDetailDto.class);
        Assertions.assertEquals(200, responseSpec.getStatusCode());
        Assertions.assertEquals("APP002", entryGetDetailDto.getStandardApplicantCode());
        Assertions.assertEquals("AD99002", entryGetDetailDto.getApplicationCode());
        Assertions.assertEquals("Rescheduled due to missing docs", entryGetDetailDto.getNotes());
        Assertions.assertEquals("CASE123457", entryGetDetailDto.getCaseReference());
        Assertions.assertFalse(entryGetDetailDto.getHasOffsiteFee());
        Assertions.assertEquals(uuids[1], entryGetDetailDto.getId());
        Assertions.assertEquals(uuids[0], entryGetDetailDto.getListId());

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "id",
                        null,
                        uuids[1].toString(),
                        AppListEntryAuditOperation.GET_APP_ENTRY_LIST_DETAIL.getType().name(),
                        AppListEntryAuditOperation.GET_APP_ENTRY_LIST_DETAIL.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS,
                        "id",
                        null,
                        uuids[0].toString(),
                        AppListEntryAuditOperation.GET_APP_ENTRY_LIST_DETAIL.getType().name(),
                        AppListEntryAuditOperation.GET_APP_ENTRY_LIST_DETAIL.getEventName()));
    }

    @Test
    public void testGetApplicationEntryListDoesNotExist() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries/"
                                        + uuids[1]),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListIsClosedExist() throws Exception {
        var tokenGenerator = createAdminToken();

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getClosedApplicationListId()
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListWithIsDeleted() throws Exception {
        var tokenGenerator = createAdminToken();

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getDeletedIdApplicationListId()
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListWithEntryNotPartOfList() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);
        UUID[] uuids2 = getValidEntryForList(VALID_ENTRY2_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT + "/" + uuids[0] + "/entries/" + uuids2[1]),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationEntryListWithEntryNotInList() throws Exception {
        var tokenGenerator = createAdminToken();

        UUID[] uuids = getValidEntryForList(VALID_ENTRY_PK);

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + uuids[0]
                                        + "/entries/"
                                        + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.ENTRY_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    public void testGetApplicationListEntriesForUnknownListReturns404() throws Exception {
        var tokenGenerator = createAdminToken();

        var responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + UUID.randomUUID() + "/entries"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(404, problemDetail.getStatus());
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getMessage(),
                problemDetail.getDetail());
    }

    @Test
    @DisplayName("GET Application List Entries persists read audit rows")
    void givenValidRequest_whenGetApplicationListEntries_thenDataAuditRowsPersisted()
            throws Exception {
        // Create a realistic list + entry so the request passes through controller, service,
        // mapper, audit listeners and finally into the DATA_AUDIT table.
        val list = createAndSaveList(OPEN);
        val applicationCode = buildApplicationCode("APPREAD");
        applicationCode.setTitle("Read audit application title");
        applicationCode.setFeeDue(YesOrNo.YES);
        applicationCode.setApplicationListEntryList(null);
        val persistedApplicationCode = persistance.save(applicationCode);

        val entry = createEntry(list);
        entry.setApplicationCode(persistedApplicationCode);
        entry.setSequenceNumber((short) 7);
        entry.setAccountNumber("ACC-123");

        val applicantAddress = new NameAddressTestData().someOrganisation();
        applicantAddress.setCode(NameAddressCodeType.APPLICANT);
        applicantAddress.setAddress1("1 Audit Street");
        applicantAddress.setName("Applicant Audit Org");
        val persistedApplicantAddress = persistance.save(applicantAddress);
        entry.setAnamedaddress(persistedApplicantAddress);

        val respondentAddress = new NameAddressTestData().someOrganisation();
        respondentAddress.setCode(NameAddressCodeType.RESPONDENT);
        respondentAddress.setAddress1("2 Audit Street");
        respondentAddress.setName("Respondent Audit Org");
        respondentAddress.setPostcode("ZZ1 1ZZ");
        val persistedRespondentAddress = persistance.save(respondentAddress);
        entry.setRnameaddress(persistedRespondentAddress);

        val persistedEntry = persistance.save(entry);
        saveResolution(persistedEntry, "RC1");

        // Remove setup-time audit rows so the assertions below only inspect the rows produced by
        // the GET /application-lists/{listId}/entries request.
        dataAuditRepository.deleteAll();

        val token = createAdminToken().fetchTokenForRole();

        // Perform the real API call with DB-backed query parameters that should each become a row
        // in DATA_AUDIT.
        val response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        token,
                        rs ->
                                rs.queryParam("applicantName", "Applicant Audit Org")
                                        .queryParam("respondentName", "Respondent Audit Org")
                                        .queryParam("respondentPostcode", "ZZ1 1ZZ")
                                        .queryParam("accountReference", "ACC-123")
                                        .queryParam(
                                                "applicationTitle", "Read audit application title")
                                        .queryParam("resulted", "RC1")
                                        .queryParam("feeRequired", true)
                                        .queryParam("sequenceNumber", 7),
                        new OpenApiPageMetaData());

        response.then().statusCode(HttpStatus.OK.value());
        response.as(EntryPage.class);

        val allAuditRows = dataAuditRepository.findAll();

        // Assert the persisted rows directly so the vertical slice proves the backend is writing
        // the legacy-style read audit data, not just logging it.
        assertAuditRow(
                allAuditRows,
                TableNames.APPLICATION_LISTS,
                "id",
                list.getUuid().toString(),
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.APPLICATION_LISTS_ENTRY,
                "ale_id",
                "0",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.NAME_ADDRESS,
                "name",
                "Applicant Audit Org",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.NAME_ADDRESS,
                "postcode",
                "ZZ1 1ZZ",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.APPLICATION_LISTS_ENTRY,
                "account_number",
                "ACC-123",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.APPLICATION_CODES,
                "application_code_title",
                "Read audit application title",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.RESOLUTION_CODES,
                "resolution_code",
                "RC1",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.APPLICATION_CODES,
                "fee_due",
                YesOrNo.YES.name(),
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
        assertAuditRow(
                allAuditRows,
                TableNames.APPLICATION_LISTS_ENTRY,
                "sequence_number",
                "7",
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST);
    }

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

        EntryGetSummaryDto entryGetSummaryDto =
                findEntryByApplicationTitle(page, "Certified genuine copy document");
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
                                .getEmail()
                                .get())
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
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        entryGetSummaryDto = findEntryByApplicationTitle(page, "Appeal to Crown Court");
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(
                        entryGetSummaryDto
                                .getRespondent()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("Flat 4, 22 Hillside");
        assertThat(entryGetSummaryDto.getRespondent().getPerson().getContactDetails().getPostcode())
                .isEqualTo("SN12 1ZZ");

        assertThat(entryGetSummaryDto.getApplicationTitle()).isEqualTo("Appeal to Crown Court");
        assertThat(entryGetSummaryDto.getLegislation())
                .isEqualTo("Section 108 Magistrates' Courts Act 1980");
        assertThat(entryGetSummaryDto.getId()).isNotNull();
        assertThat(entryGetSummaryDto.getIsFeeRequired()).isFalse();
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2025-04-21"));
        assertThat(entryGetSummaryDto.getListId()).isNotNull();
    }

    private EntryGetSummaryDto findEntryByApplicationTitle(
            EntryPage page, String applicationTitle) {
        return page.getContent().stream()
                .filter(entry -> applicationTitle.equals(entry.getApplicationTitle()))
                .findFirst()
                .orElseThrow();
    }

    private void assertAuditRow(
            List<DataAudit> auditRows,
            String tableName,
            String columnName,
            String newValue,
            AppListEntryAuditOperation operation) {
        val row =
                auditRows.stream()
                        .filter(audit -> tableName.equals(audit.getTableName()))
                        .filter(audit -> columnName.equals(audit.getColumnName()))
                        .filter(audit -> newValue.equals(audit.getNewValue()))
                        .filter(audit -> operation.getType().equals(audit.getUpdateType()))
                        .filter(audit -> operation.getEventName().equals(audit.getEventName()))
                        .findFirst()
                        .orElseThrow();

        assertThat(row.getOldValue()).isEmpty();
        assertThat(row.getLink()).isNotBlank();
        assertThat(row.getCreatedUser()).isNotBlank();
    }

    @StabilityTest
    public void testGetApplicationEntriesSearchWithAllDetails() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setDate(LocalDate.parse("2024-04-21"));
        filterDto.setApplicantSurname("rne");
        filterDto.setAccountReference("29345");
        filterDto.setStatus(ApplicationListStatus.OPEN);
        filterDto.setCjaCode("CJ");
        filterDto.setCourtCode("RCJ001");
        filterDto.setOtherLocationDescription("oth");
        filterDto.setRespondentOrganisation("Sarah");
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
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstName())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getLastName())
                .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getMiddleName().get())
                .isEqualTo("Francis Michael");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getMiddleName().get())
                .isNull();

        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("1 Market Street");
        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getEmail()
                                .get())
                .isEqualTo("john.smith@example.com");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getPostcode())
                .isEqualTo("AB11 2CD");
        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getPhone()
                                .get())
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
                                .getEmail()
                                .get())
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
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2024-04-21"));
        assertThat(entryGetSummaryDto.getListId()).isNotNull();
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
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstName())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getLastName())
                .isEqualTo("Turner");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getMiddleName().get())
                .isEqualTo("Francis Michael");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getMiddleName().get())
                .isNull();

        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("1 Market Street");
        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getEmail()
                                .get())
                .isEqualTo("john.smith@example.com");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getPostcode())
                .isEqualTo("AB11 2CD");
        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getPhone()
                                .get())
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
                                .getEmail()
                                .get())
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
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2024-04-21"));
        assertThat(entryGetSummaryDto.getListId()).isNotNull();
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
                                ApplicationEntrySortFieldEnum.APPLICATION_TITLE.getApiValue()
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
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstName())
                .isEqualTo("Jane");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getLastName())
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
                                .getEmail()
                                .get())
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
        assertThat(entryGetSummaryDto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        entryGetSummaryDto = page.getContent().get(4);
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getFirstName())
                .isEqualTo("John");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getName().getLastName())
                .isEqualTo("Turner");

        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getAddressLine1())
                .isEqualTo("1 Market Street");
        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getEmail()
                                .get())
                .isEqualTo("john.smith@example.com");
        assertThat(entryGetSummaryDto.getApplicant().getPerson().getContactDetails().getPostcode())
                .isEqualTo("AB11 2CD");
        assertThat(
                        entryGetSummaryDto
                                .getApplicant()
                                .getPerson()
                                .getContactDetails()
                                .getPhone()
                                .get())
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
                                .getEmail()
                                .get())
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
        assertThat(entryGetSummaryDto.getDate()).isEqualTo(LocalDate.parse("2024-04-21"));
        assertThat(entryGetSummaryDto.getListId()).isNotNull();
    }

    @StabilityTest
    public void
            givenApplicationListEntrySuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
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
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());
            Assertions.assertEquals(
                    applicationEntrySortFieldEnum.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());
            responseSpec.then().statusCode(200);
        }

        Assertions.assertTrue(ApplicationListSortFieldEnum.values().length > 0);
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
        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), responseSpec);
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
    @StabilityTest
    public void testGetApplicationEntriesReturnsAllResultCodes() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);
        ApplicationCode applicationCode = createApplicationCode("APP002", true);

        ApplicationListEntry entry = createEntry(list);
        entry.setApplicationCode(applicationCode);
        entry.setAccountNumber("ACC123");
        entry = persistance.save(entry);

        saveResolutions(entry, "RC1", "RC2");

        Response responseSpec = executeGetEntries(list.getUuid(), 20, 0);

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        EntryGetSummaryDto dto = findEntry(page, entry.getUuid());

        assertResultCodes(dto, "RC1", "RC2");
    }

    @Test
    public void testGetApplicationListEntriesWithInvalidSequenceNumberReturnsWholeNumberMessage()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries?sequenceNumber=NaN"
                                        + "&pageNumber=0&pageSize=10&sort=sequenceNumber,asc"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);

        String expectedJson =
                """
                {"type":"COMMON-11","title":"Method Error","status":400,"detail":"Validation failed for fields:",
                "errors":{"sequenceNumber":"Please ensure sequenceNumber is a whole number"}}
                """;

        JSONAssert.assertEquals(expectedJson, responseSpec.asString(), false);
    }

    @Test
    public void
            testGetApplicationListEntriesWithSpecialCharacterSequenceNumberReturnsWholeNumberMessage()
                    throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries?sequenceNumber=1;--"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);

        String expectedJson =
                """
                {"type":"COMMON-11","title":"Method Error","status":400,"detail":"Validation failed for fields:",
                "errors":{"sequenceNumber":"Please ensure sequenceNumber is a whole number"}}
                """;

        JSONAssert.assertEquals(expectedJson, responseSpec.asString(), false);
    }

    @Test
    public void testGetApplicationListEntriesWithInvalidRespondentPostcodeReturnsValidationError()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + UUID.randomUUID() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentPostcode", "@£1 1@£"));

        responseSpec.then().statusCode(400);
        responseSpec
                .then()
                .body("errors.respondentPostcode", Matchers.containsString("must match"));
    }

    @Test
    public void testGetApplicationListEntriesWithInvalidFeeRequiredReturnsBooleanMessage()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.randomUUID()
                                        + "/entries?feeRequired=maybe"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);

        String expectedJson =
                """
                {"type":"COMMON-11","title":"Method Error","status":400,"detail":"Validation failed for fields:",
                "errors":{"feeRequired":"Please ensure feeRequired is a valid boolean value"}}
                """;

        JSONAssert.assertEquals(expectedJson, responseSpec.asString(), false);
    }

    @Test
    public void testGetApplicationListEntriesWithInvalidApplicationTitleReturnsValidationError()
            throws Exception {
        assertGetApplicationListEntriesInvalidFilterReturnsValidationError(
                "applicationTitle", "Title;--");
    }

    @Test
    public void testGetApplicationListEntriesWithInvalidApplicantNameReturnsValidationError()
            throws Exception {
        assertGetApplicationListEntriesInvalidFilterReturnsValidationError(
                "applicantName", "Jane#");
    }

    @Test
    public void testGetApplicationListEntriesWithInvalidRespondentNameReturnsValidationError()
            throws Exception {
        assertGetApplicationListEntriesInvalidFilterReturnsValidationError(
                "respondentName", "Smith<>");
    }

    @Test
    public void testGetApplicationListEntriesFiltersByPartialRespondentPostcodeHit()
            throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry matchingEntry = createEntry(list);
        setRespondentName(matchingEntry, "Mr", "Partial", "Match");
        matchingEntry.getRnameaddress().setPostcode("SW1A 1AA");
        matchingEntry.setSequenceNumber((short) 1);
        matchingEntry = persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(list);
        setRespondentName(nonMatchingEntry, "Ms", "Partial", "Miss");
        nonMatchingEntry.getRnameaddress().setPostcode("XY9 8ZZ");
        nonMatchingEntry.setSequenceNumber((short) 2);
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentPostcode", "sw1"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByPartialRespondentPostcodeMiss()
            throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry entry = createEntry(list);
        setRespondentName(entry, "Mr", "Partial", "Miss");
        entry.getRnameaddress().setPostcode("SW1A 1AA");
        entry.setSequenceNumber((short) 1);
        persistance.save(entry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentPostcode", "ZZ9"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(0, page.getContent().size());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByAnyAppliedResultCode() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);
        ApplicationCode applicationCode = createApplicationCode("APP002", true);

        ApplicationListEntry entry = createEntry(list);
        entry.setApplicationCode(applicationCode);
        entry = persistance.save(entry);

        saveResolutions(entry, "RC1", "RC2");

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("resulted", "RC1"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(entry.getUuid(), page.getContent().getFirst().getId());
        assertResultCodes(page.getContent().getFirst(), "RC1", "RC2");
    }

    @Test
    public void testGetApplicationListEntriesFiltersByPartialResultCode() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry matchingEntry = createEntry(list);
        matchingEntry.setApplicationCode(createApplicationCode("APP002", true));
        matchingEntry.setSequenceNumber((short) 1);
        matchingEntry = persistance.save(matchingEntry);
        saveResolutions(matchingEntry, "APPC");

        ApplicationListEntry nonMatchingEntry = createEntry(list);
        nonMatchingEntry.setApplicationCode(createApplicationCode("APP003", true));
        nonMatchingEntry.setSequenceNumber((short) 2);
        nonMatchingEntry = persistance.save(nonMatchingEntry);
        saveResolutions(nonMatchingEntry, "RC1");

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("sequenceNumber,asc"),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("resulted", "AP"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
        assertResultCodes(page.getContent().getFirst(), "APPC");
    }

    @Test
    public void testGetApplicationListEntriesTrimsAccountReferenceFilter() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry matchingEntry = createEntry(list);
        matchingEntry.setAccountNumber("E40-123");
        matchingEntry.setSequenceNumber((short) 1);
        matchingEntry = persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(list);
        nonMatchingEntry.setAccountNumber("ABC-123");
        nonMatchingEntry.setSequenceNumber((short) 2);
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("accountReference", " E40 "),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesIgnoresBlankAccountReferenceFilter() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry firstEntry = createEntry(list);
        firstEntry.setAccountNumber("E40-123");
        firstEntry.setSequenceNumber((short) 1);
        firstEntry = persistance.save(firstEntry);

        ApplicationListEntry secondEntry = createEntry(list);
        secondEntry.setAccountNumber("ABC-123");
        secondEntry.setSequenceNumber((short) 2);
        secondEntry = persistance.save(secondEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("sequenceNumber,asc"),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("accountReference", " "),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(2, page.getContent().size());
        Assertions.assertEquals(firstEntry.getUuid(), page.getContent().get(0).getId());
        Assertions.assertEquals(secondEntry.getUuid(), page.getContent().get(1).getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByResultCodeAcrossEntriesWhenNotLatest()
            throws Exception {
        ApplicationList list = createAndSaveList(OPEN);
        ApplicationListEntry latestMatchingEntry = createEntry(list);
        latestMatchingEntry.setApplicationCode(createApplicationCode("APP002", true));
        latestMatchingEntry.setSequenceNumber((short) 1);
        latestMatchingEntry = persistance.save(latestMatchingEntry);
        saveResolutions(latestMatchingEntry, "RC1");

        ApplicationListEntry historicalMatchingEntryOne = createEntry(list);
        historicalMatchingEntryOne.setApplicationCode(createApplicationCode("APP003", true));
        historicalMatchingEntryOne.setSequenceNumber((short) 2);
        historicalMatchingEntryOne = persistance.save(historicalMatchingEntryOne);
        saveResolutions(historicalMatchingEntryOne, "RC1", "RC2");

        ApplicationListEntry historicalMatchingEntryTwo = createEntry(list);
        historicalMatchingEntryTwo.setApplicationCode(createApplicationCode("APP004", true));
        historicalMatchingEntryTwo.setSequenceNumber((short) 3);
        historicalMatchingEntryTwo = persistance.save(historicalMatchingEntryTwo);
        saveResolutions(historicalMatchingEntryTwo, "RC1", "RC3");

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("sequenceNumber,asc"),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("resulted", "RC1"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        EntryPage page = responseSpec.as(EntryPage.class);

        Assertions.assertEquals(3, page.getContent().size());
        Assertions.assertEquals(3, page.getTotalElements());
        Assertions.assertEquals(latestMatchingEntry.getUuid(), page.getContent().get(0).getId());
        Assertions.assertEquals(
                historicalMatchingEntryOne.getUuid(), page.getContent().get(1).getId());
        Assertions.assertEquals(
                historicalMatchingEntryTwo.getUuid(), page.getContent().get(2).getId());
        assertResultCodes(page.getContent().get(0), "RC1");
        assertResultCodes(page.getContent().get(1), "RC1", "RC2");
        assertResultCodes(page.getContent().get(2), "RC1", "RC3");
    }

    @Test
    public void testGetApplicationListEntriesFiltersByApplicantNameOnly() throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        // matches applicant filter
        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setApplicantName(matchingEntry, "Mr", "John", "Turner");
        setRespondentName(matchingEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(matchingEntry);

        // same respondent, different applicant, should not match
        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        setApplicantName(nonMatchingEntry, "Ms", "Jane", "Smith");
        setRespondentName(nonMatchingEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "John Turner"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByApplicantNamePartialForename()
            throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setApplicantName(matchingEntry, "Mr", "John", "Turner");
        persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        setApplicantName(nonMatchingEntry, "Ms", "Jane", "Smith");
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "John"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByApplicantNamePartialSurname()
            throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setApplicantName(matchingEntry, "Mr", "John", "Turner");
        persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        setApplicantName(nonMatchingEntry, "Ms", "Jane", "Smith");
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "Turner"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByStandardApplicantName() throws Exception {
        final ApplicationList applicationList = createAndSaveList(Status.OPEN);

        StandardApplicant matchingApplicant = new StandardApplicantTestData().someComplete();
        matchingApplicant.setName(null);
        matchingApplicant.setApplicantForename1("Jane");
        matchingApplicant.setApplicantSurname("Doe");
        matchingApplicant = persistance.save(matchingApplicant);
        ApplicationListEntry matchingEntry = createEntry(applicationList);
        matchingEntry.setStandardApplicant(matchingApplicant);
        persistance.save(matchingEntry);

        StandardApplicant nonMatchingApplicant = new StandardApplicantTestData().someComplete();
        nonMatchingApplicant.setName(null);
        nonMatchingApplicant.setApplicantForename1("John");
        nonMatchingApplicant.setApplicantSurname("Smith");
        nonMatchingApplicant = persistance.save(nonMatchingApplicant);
        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        nonMatchingEntry.setStandardApplicant(nonMatchingApplicant);
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "Jane Doe"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByStandardApplicantNamePartialForename()
            throws Exception {
        final ApplicationList applicationList = createAndSaveList(Status.OPEN);

        StandardApplicant matchingApplicant = new StandardApplicantTestData().someComplete();
        matchingApplicant.setName(null);
        matchingApplicant.setApplicantForename1("Jane");
        matchingApplicant.setApplicantSurname("Doe");
        matchingApplicant = persistance.save(matchingApplicant);
        ApplicationListEntry matchingEntry = createEntry(applicationList);
        matchingEntry.setStandardApplicant(matchingApplicant);
        persistance.save(matchingEntry);

        StandardApplicant nonMatchingApplicant = new StandardApplicantTestData().someComplete();
        nonMatchingApplicant.setName(null);
        nonMatchingApplicant.setApplicantForename1("John");
        nonMatchingApplicant.setApplicantSurname("Smith");
        nonMatchingApplicant = persistance.save(nonMatchingApplicant);
        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        nonMatchingEntry.setStandardApplicant(nonMatchingApplicant);
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "Jane"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByStandardApplicantNamePartialSurname()
            throws Exception {
        final ApplicationList applicationList = createAndSaveList(Status.OPEN);

        StandardApplicant matchingApplicant = new StandardApplicantTestData().someComplete();
        matchingApplicant.setName(null);
        matchingApplicant.setApplicantForename1("Jane");
        matchingApplicant.setApplicantSurname("Doe");
        matchingApplicant = persistance.save(matchingApplicant);
        ApplicationListEntry matchingEntry = createEntry(applicationList);
        matchingEntry.setStandardApplicant(matchingApplicant);
        persistance.save(matchingEntry);

        StandardApplicant nonMatchingApplicant = new StandardApplicantTestData().someComplete();
        nonMatchingApplicant.setName(null);
        nonMatchingApplicant.setApplicantForename1("John");
        nonMatchingApplicant.setApplicantSurname("Smith");
        nonMatchingApplicant = persistance.save(nonMatchingApplicant);
        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        nonMatchingEntry.setStandardApplicant(nonMatchingApplicant);
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "Doe"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByStandardApplicantName_returnsExpectedJson()
            throws Exception {
        final ApplicationList applicationList = createAndSaveList(Status.OPEN);

        StandardApplicant matchingApplicant = new StandardApplicantTestData().someComplete();
        matchingApplicant.setName(null);
        matchingApplicant.setApplicantTitle("Ms");
        matchingApplicant.setApplicantForename1("Jane");
        matchingApplicant.setApplicantSurname("Doe");
        matchingApplicant = persistance.save(matchingApplicant);

        ApplicationListEntry matchingEntry = createEntry(applicationList);
        matchingEntry.setStandardApplicant(matchingApplicant);
        matchingEntry.setSequenceNumber((short) 3);
        persistance.save(matchingEntry);

        StandardApplicant nonMatchingApplicant = new StandardApplicantTestData().someComplete();
        nonMatchingApplicant.setName(null);
        nonMatchingApplicant.setApplicantForename1("John");
        nonMatchingApplicant.setApplicantSurname("Smith");
        nonMatchingApplicant = persistance.save(nonMatchingApplicant);

        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        nonMatchingEntry.setStandardApplicant(nonMatchingApplicant);
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("applicantName", "Jane Doe"),
                        new OpenApiPageMetaData());

        responseSpec
                .then()
                .statusCode(200)
                .body("pageNumber", Matchers.equalTo(0))
                .body("pageSize", Matchers.equalTo(10))
                .body("totalElements", Matchers.equalTo(1))
                .body("content.size()", Matchers.equalTo(1))
                .body("content[0].id", Matchers.equalTo(matchingEntry.getUuid().toString()))
                .body("content[0].listId", Matchers.equalTo(applicationList.getUuid().toString()))
                .body("content[0].sequenceNumber", Matchers.equalTo(3))
                .body("content[0].applicant.person.name.title", Matchers.equalTo("Ms"))
                .body("content[0].applicant.person.name.firstName", Matchers.equalTo("Jane"))
                .body("content[0].applicant.person.name.lastName", Matchers.equalTo("Doe"));
    }

    @Test
    public void testGetApplicationListEntriesFiltersByRespondentNameOnly() throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        // matches respondent filter
        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setApplicantName(matchingEntry, "Mr", "John", "Turner");
        setRespondentName(matchingEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(matchingEntry);

        // same applicant, different respondent, should not match
        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        setApplicantName(nonMatchingEntry, "Mr", "John", "Turner");
        setRespondentName(nonMatchingEntry, "Mr", "Bob", "Brown");
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentName", "Sarah Johnson"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByRespondentNamePartialForename()
            throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setRespondentName(matchingEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        setRespondentName(nonMatchingEntry, "Mr", "Bob", "Brown");
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentName", "Sarah"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByRespondentNamePartialSurname()
            throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setRespondentName(matchingEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(matchingEntry);

        ApplicationListEntry nonMatchingEntry = createEntry(applicationList);
        setRespondentName(nonMatchingEntry, "Mr", "Bob", "Brown");
        persistance.save(nonMatchingEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("respondentName", "Johnson"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    public void testGetApplicationListEntriesFiltersByApplicantNameAndRespondentName()
            throws Exception {
        ApplicationList applicationList = createAndSaveList(Status.OPEN);

        // matches both filters
        ApplicationListEntry matchingEntry = createEntry(applicationList);
        setApplicantName(matchingEntry, "Mr", "John", "Turner");
        setRespondentName(matchingEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(matchingEntry);

        // matches applicant only
        ApplicationListEntry applicantOnlyEntry = createEntry(applicationList);
        setApplicantName(applicantOnlyEntry, "Ms", "Jane", "Turner");
        setRespondentName(applicantOnlyEntry, "Mr", "Bob", "Brown");
        persistance.save(applicantOnlyEntry);

        // matches respondent only
        ApplicationListEntry respondentOnlyEntry = createEntry(applicationList);
        setApplicantName(respondentOnlyEntry, "Ms", "Jane", "Smith");
        setRespondentName(respondentOnlyEntry, "Mrs", "Sarah", "Johnson");
        persistance.save(respondentOnlyEntry);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + applicationList.getUuid()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs ->
                                rs.queryParam("applicantName", "John Turner")
                                        .queryParam("respondentName", "Sarah Johnson"),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(matchingEntry.getUuid(), page.getContent().getFirst().getId());
    }

    @Test
    @StabilityTest
    public void testGetApplicationListEntriesSortMetadata() throws Exception {
        var tokenGenerator = createAdminToken();

        ApplicationList list = createAndSaveList(OPEN);

        Assertions.assertTrue(ApplicationEntryByListIdSortFieldEnum.values().length > 0);

        for (ApplicationEntryByListIdSortFieldEnum sortField :
                ApplicationEntryByListIdSortFieldEnum.values()) {

            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(sortField.getApiValue() + ",desc"),
                            getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                            tokenGenerator.fetchTokenForRole());

            responseSpec.then().statusCode(200);

            EntryPage page = responseSpec.as(EntryPage.class);

            assertEquals(1, page.getSort().getOrders().size());
            assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().getFirst().getDirection());
            assertEquals(
                    sortField.getApiValue(), page.getSort().getOrders().getFirst().getProperty());
        }
    }

    @Test
    @StabilityTest
    public void testGetApplicationListEntriesSortsByApplicantName() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry zoe = createEntry(list);
        setApplicantName(zoe, "Dr", "Zoe", "Anderson");
        persistance.save(zoe);

        ApplicationListEntry amy = createEntry(list);
        setApplicantName(amy, "Mr", "Amy", "Zimmer");
        persistance.save(amy);

        ApplicationListEntry bob = createEntry(list);
        setApplicantName(bob, "Ms", "Bob", "Brown");
        persistance.save(bob);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(
                                SortableField.getSortStringForAsc(
                                        ApplicationEntryByListIdSortFieldEnum.APPLICANT)),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);

        List<String> applicantNames =
                page.getContent().stream()
                        .map(this::renderApplicantName)
                        .map(String::toLowerCase)
                        .toList();

        Assertions.assertEquals(
                List.of("mr amy zimmer", "ms bob brown", "dr zoe anderson"), applicantNames);
    }

    @Test
    @StabilityTest
    public void testGetApplicationListEntriesSortsByStandardApplicantDisplayName()
            throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry zoe = createEntry(list);
        zoe.setStandardApplicant(createStandardApplicantPerson("APPZOE", "Dr", "Zoe", "Anderson"));
        persistance.save(zoe);

        ApplicationListEntry amy = createEntry(list);
        amy.setStandardApplicant(createStandardApplicantPerson("APPAMY", "Mr", "Amy", "Zimmer"));
        persistance.save(amy);

        ApplicationListEntry betaOrg = createEntry(list);
        betaOrg.setStandardApplicant(createStandardApplicantOrganisation("APPORG", "Beta Org"));
        persistance.save(betaOrg);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(
                                SortableField.getSortStringForAsc(
                                        ApplicationEntryByListIdSortFieldEnum.APPLICANT)),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);

        List<String> applicantNames =
                page.getContent().stream()
                        .map(this::renderApplicantDisplayName)
                        .map(String::toLowerCase)
                        .toList();

        Assertions.assertEquals(
                List.of("mr amy zimmer", "beta org", "dr zoe anderson"), applicantNames);
    }

    @Test
    @StabilityTest
    public void testGetApplicationListEntriesSortsByRespondentName() throws Exception {
        ApplicationList list = createAndSaveList(OPEN);

        ApplicationListEntry zoe = createEntry(list);
        setRespondentName(zoe, "Dr", "Zoe", "Anderson");
        zoe.getRnameaddress().setDateOfBirth(LocalDate.of(1990, 1, 1));
        persistance.save(zoe);

        ApplicationListEntry amy = createEntry(list);
        setRespondentName(amy, "Mr", "Amy", "Zimmer");
        amy.getRnameaddress().setDateOfBirth(LocalDate.of(1985, 5, 5));
        persistance.save(amy);

        ApplicationListEntry bob = createEntry(list);
        setRespondentName(bob, "Ms", "Bob", "Brown");
        bob.getRnameaddress().setDateOfBirth(LocalDate.of(1975, 9, 9));
        persistance.save(bob);

        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(
                                SortableField.getSortStringForAsc(
                                        ApplicationEntryByListIdSortFieldEnum.RESPONDENT)),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + list.getUuid() + "/entries"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        EntryPage page = responseSpec.as(EntryPage.class);

        List<String> respondentNames =
                page.getContent().stream()
                        .map(this::renderRespondentName)
                        .map(String::toLowerCase)
                        .toList();

        Assertions.assertEquals(
                List.of("mr amy zimmer", "ms bob brown", "dr zoe anderson"), respondentNames);

        List<LocalDate> respondentDobs =
                page.getContent().stream()
                        .map(dto -> dto.getRespondent().getPerson().getDateOfBirth())
                        .toList();

        Assertions.assertEquals(
                List.of(
                        LocalDate.of(1985, 5, 5),
                        LocalDate.of(1975, 9, 9),
                        LocalDate.of(1990, 1, 1)),
                respondentDobs);
    }

    private void assertGetApplicationListEntriesInvalidFilterReturnsValidationError(
            String fieldName, String fieldValue) throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + UUID.randomUUID() + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam(fieldName, fieldValue));

        responseSpec.then().statusCode(400);
        responseSpec.then().body("errors." + fieldName, Matchers.containsString("must match"));
    }

    private StandardApplicant createStandardApplicantPerson(
            String applicantCode, String title, String forename, String surname) {
        StandardApplicant applicant = new StandardApplicantTestData().someComplete();
        applicant.setApplicantCode(applicantCode);
        applicant.setName(null);
        applicant.setApplicantTitle(title);
        applicant.setApplicantForename1(forename);
        applicant.setApplicantSurname(surname);
        return persistance.save(applicant);
    }

    private StandardApplicant createStandardApplicantOrganisation(
            String applicantCode, String name) {
        StandardApplicant applicant = new StandardApplicantTestData().someComplete();
        applicant.setApplicantCode(applicantCode);
        applicant.setName(name);
        applicant.setApplicantTitle(null);
        applicant.setApplicantForename1(null);
        applicant.setApplicantSurname(null);
        return persistance.save(applicant);
    }

    private String renderApplicantDisplayName(EntryGetSummaryDto dto) {
        if (dto.getApplicant() == null) {
            return "";
        }

        if (dto.getApplicant().getOrganisation() != null) {
            return dto.getApplicant().getOrganisation().getName();
        }

        return renderApplicantName(dto);
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
