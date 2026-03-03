package uk.gov.hmcts.appregister.controller.applicationentry;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public abstract class AbstractApplicationEntryCrudTest extends BaseIntegration {

    protected static final String WEB_CONTEXT = "application-list-entries";
    protected static final String CREATE_ENTRY_CONTEXT = "application-lists";

    // The total app entries inserted by flyway scripts
    protected static final int TOTAL_APP_ENTRY_COUNT = 11;

    // The deleted list that has been inserted by the flyway scripts
    protected static final long DELETED_LIST_PK = 12;

    // A valid list with a valid entry primary key
    protected static final long VALID_ENTRY_PK = 2;
    protected static final long VALID_ENTRY2_PK = 5;

    @Value("${spring.sql.init.schema-locations}")
    protected String sqlInitSchemaLocations;

    @Value("${spring.data.web.pageable.default-page-size}")
    protected Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    protected Integer maxPageSize;

    @Autowired protected TransactionalUnitOfWork unitOfWork;
    @Autowired protected ApplicationListRepository applicationListRepository;
    @Autowired protected ApplicationListEntryRepository applicationListEntryRepository;

    /** Build a token generator with ADMIN role. */
    protected TokenGenerator createAdminToken() {
        return getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
    }

    protected UUID getOpenApplicationListId() {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .getFirst();
                    return applicationList.getUuid();
                });
    }

    protected UUID getClosedApplicationListId() {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .get(2);
                    return applicationList.getUuid();
                });
    }

    protected UUID getDeletedIdApplicationListId() {
        return unitOfWork.inTransaction(
                () -> applicationListRepository.findById(DELETED_LIST_PK).get().getUuid());
    }

    /**
     * gets the uuids for a valid application entry inside a list.
     *
     * @param entryId The entry id pk that is inside the list
     * @return The uuids of the entry and list
     */
    protected UUID[] getValidEntryForList(Long entryId) {
        return unitOfWork.inTransaction(
                () -> {
                    Optional<ApplicationListEntry> applicationListEntry =
                            applicationListEntryRepository.findById(entryId);

                    Assertions.assertTrue(applicationListEntry.isPresent());

                    return new UUID[] {
                        applicationListEntry.get().getApplicationList().getUuid(),
                        applicationListEntry.get().getUuid()
                    };
                });
    }

    protected record ApplicationEntryFilter(
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
        public RequestSpecification apply(RequestSpecification rs) {
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

    // ---- HTTP helpers ----

    /**
     * Creates an entry using provided DTO and unique surname (overwrites DTO surname). Asserts
     * creation status (201) and returns parsed EntryGetDetailDto + Response.
     */
    protected SuccessCreateEntryResponse createEntryWithUniqueSurname(
            TokenGenerator tokenGenerator, EntryCreateDto entryCreateDto, String uniqueSurname)
            throws Exception {

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

        responseSpecCreate.then().statusCode(201);
        Assertions.assertNotNull(HeaderUtil.getETag(responseSpecCreate));

        return new SuccessCreateEntryResponse(
                responseSpecCreate.as(EntryGetDetailDto.class), responseSpecCreate);
    }

    /** Finds entries by surname using the application entry filter and returns EntryPage. */
    protected EntryPage findEntriesBySurname(
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

        responseFindEntrySpec.then().statusCode(200);
        return responseFindEntrySpec.as(EntryPage.class);
    }

    /** Calls the GET paging endpoint without filters and returns EntryPage. */
    protected EntryPage findAllEntriesWithLargePage(
            TokenGenerator tokenGenerator, int size, int page) throws Exception {

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

    // ---- Validation helpers ----

    protected void validateEntryCreationResponse(
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

    protected void validateEntryUpdateResponse(
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

    public Response createListEntryWithAllData() throws Exception {
        return createListEntryWithAllData(null);
    }

    // Convenience: create a full entry
    public Response createListEntryWithAllData(Consumer<EntryCreateDto> consumeBeforeCommit)
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        if (consumeBeforeCommit != null) {
            consumeBeforeCommit.accept(entryCreateDto);
        }

        String surnameToLookup = UUID.randomUUID().toString();
        entryCreateDto.getApplicant().getPerson().getName().setSurname(surnameToLookup);

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(201);

        EntryGetDetailDto createdDto = responseSpecCreate.as(EntryGetDetailDto.class);

        validateEntryCreationResponse(
                entryCreateDto, createdDto, List.of("Premises Address", "Premises Date"));

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

        responseFindEntrySpec.then().statusCode(200);

        EntryPage page = responseFindEntrySpec.as(EntryPage.class);
        PagingAssertionUtil.assertPageDetails(page, 10, 0, 1, 1);
        Assertions.assertEquals(createdDto.getId(), page.getContent().getFirst().getId());

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

    public record SuccessCreateEntryResponse(EntryGetDetailDto getDetailDto, Response response) {}

    EntryUpdateDto getCorrectUpdateDataDto() {
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
        updateDto.setOfficials(officials);

        updateDto.setApplicationCode("ZS99007");
        updateDto.setHasOffsiteFee(true);

        updateDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Premises Address", "test wording"),
                        new TemplateSubstitution("Premises Date", LocalDate.now().toString())));

        // Ensure rule compliance
        CreateEntryDtoUtil.sanitiseFeeStatusesForDueRule(updateDto.getFeeStatuses());

        return updateDto;
    }
}
