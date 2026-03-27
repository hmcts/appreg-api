package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.common.enumeration.YesOrNo.NO;

import com.nimbusds.jose.JOSEException;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.TemplateAssertion;
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

    @MockitoBean protected UserProvider provider;

    @Autowired protected TransactionalUnitOfWork unitOfWork;
    @Autowired protected ApplicationListRepository applicationListRepository;
    @Autowired protected ApplicationListEntryRepository applicationListEntryRepository;
    @Autowired protected ApplicationCodeRepository applicationCodeRepository;

    protected static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    protected static final LocalTime TEST_TIME = LocalTime.of(10, 30);
    protected static final String VALID_COURT_CODE = "CCC003";

    @BeforeEach
    void setupUser() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

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

    protected record ApplicationEntryFilterByApplicationId(
            UUID applicationId,
            Optional<String> applicantName,
            Optional<String> respondentName,
            Optional<String> respondentPostcode,
            Optional<String> accountReference,
            Optional<String> applicationTitle,
            Optional<Boolean> feeRequired,
            Optional<Integer> sequenceNumber)
            implements UnaryOperator<RequestSpecification> {

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            rs = rs.queryParam("applicationId", applicationId.toString());
            if (applicantName.isPresent()) {
                rs = rs.queryParam("applicantName", applicantName.get());
            }
            if (respondentName.isPresent()) {
                rs = rs.queryParam("respondentName", respondentName.get());
            }
            if (respondentPostcode.isPresent()) {
                rs = rs.queryParam("respondentPostcode", respondentPostcode.get());
            }
            if (accountReference.isPresent()) {
                rs = rs.queryParam("accountReference", accountReference.get());
            }
            if (applicationTitle.isPresent()) {
                rs = rs.queryParam("applicationTitle", applicationTitle.get());
            }
            if (feeRequired.isPresent()) {
                rs = rs.queryParam("feeRequired", feeRequired.get());
            }
            if (sequenceNumber.isPresent()) {
                rs = rs.queryParam("sequenceNumber", sequenceNumber.get());
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
            EntryCreateDto entryCreateDto, EntryGetDetailDto response, String wordingSpec) {

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
        Assertions.assertNotNull(response.getWording());

        // assert the details of the template within the response
        TemplateAssertion.assertTemplateWithValues(
                wordingSpec, entryCreateDto.getWordingFields(), response.getWording());

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
            String wordingSpec,
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

        TemplateAssertion.assertTemplateWithValues(
                wordingSpec, entryUpdateDto.getWordingFields(), response.getWording());

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

        String surnameToLookup = Instancio.gen().string().get();
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
                entryCreateDto,
                createdDto,
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}");

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
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "id",
                        "",
                        null,
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        null,
                        "1",
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "ale_id",
                        "",
                        null,
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getType().name(),
                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST.getEventName()));

        return responseSpecCreate;
    }

    protected EntryUpdateDto getCorrectUpdateDataDto() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        final EntryUpdateDto updateDto =
                Instancio.of(EntryUpdateDto.class).withSettings(settings).create();

        final List<Official> officials = Instancio.ofList(Official.class).size(4).create();

        updateDto.getApplicant().setPerson(null);
        updateDto.getApplicant().getOrganisation().getContactDetails().setPostcode("AA13 1BB");
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@org.com"));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setPhone(JsonNullable.of(null));
        updateDto
                .getApplicant()
                .getOrganisation()
                .getContactDetails()
                .setMobile(JsonNullable.of(null));

        updateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA12 1AA");
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("test@test.com"));
        updateDto.getRespondent().getPerson().getName().setSecondForename(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getName().setThirdForename(JsonNullable.of(null));

        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        updateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of(null));

        updateDto.getRespondent().getPerson().getContactDetails().setPhone(JsonNullable.of(null));
        updateDto.getRespondent().getPerson().getContactDetails().setMobile(JsonNullable.of(null));

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

    public record SuccessCreateEntryResponse(EntryGetDetailDto getDetailDto, Response response) {}

    protected ApplicationListEntry createEntry(ApplicationList list) {
        return new AppListEntryTestData().someMinimal().applicationList(list).build();
    }

    // ---- data helpers ----
    public ApplicationList createAndSaveList(Status status) {
        var list = new AppListTestData().someMinimal().status(status).build();
        persistance.save(list);
        return list;
    }

    public void saveResolution(ApplicationListEntry sourceEntry, String resultCode) {
        ApplicationListEntry persistedEntry =
                applicationListEntryRepository.findById(sourceEntry.getId()).orElseThrow();

        ApplicationCode persistedCode =
                applicationCodeRepository
                        .findById(sourceEntry.getApplicationCode().getId())
                        .orElseThrow();

        ApplicationList persistedList =
                applicationListRepository
                        .findById(persistedEntry.getApplicationList().getId())
                        .orElseThrow();

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

    public static @NotNull ApplicationCode getApplicationCode(ApplicationCode persistedCode) {
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

    public static @NotNull ApplicationListEntry getApplicationListEntry(
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

    public ApplicationCode buildApplicationCode(String code) {
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

    public ApplicationList createOpenApplicationList() {
        ApplicationList list = new ApplicationList();
        list.setDate(LocalDate.now());
        list.setTime(LocalTime.of(9, 0));
        list.setStatus(OPEN);
        list.setDescription("Test list description");
        return persistance.save(list);
    }

    public ApplicationCode createApplicationCode(String code, boolean clearEntries) {
        ApplicationCode applicationCode = buildApplicationCode(code);
        if (clearEntries) {
            applicationCode.setApplicationListEntryList(null);
        }
        return persistance.save(applicationCode);
    }

    public ApplicationListEntry createApplicationListEntry(
            ApplicationList list, ApplicationCode applicationCode, String accountNumber) {
        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setApplicationList(list);
        entry.setApplicationCode(applicationCode);
        entry.setApplicationListEntryWording("Test entry wording");
        entry.setEntryRescheduled("N");
        entry.setSequenceNumber((short) 1);
        entry.setLodgementDate(LocalDate.now());
        entry.setCreatedUser("email");
        entry.setAccountNumber(accountNumber);
        entry.setCaseReference("CASE123");
        entry.setBulkUpload("N");
        entry.setRetryCount("0");
        entry.setNotes("Test notes");
        entry.setTcepStatus("NW");
        return persistance.save(entry);
    }

    public void saveResolutions(ApplicationListEntry entry, String... resultCodes) {
        ApplicationListEntry currentEntry = entry;
        for (String resultCode : resultCodes) {
            currentEntry =
                    applicationListEntryRepository.findById(currentEntry.getId()).orElseThrow();
            saveResolution(currentEntry, resultCode);
        }
    }

    public Response executeGetEntries(UUID listUuid, int size, int page)
            throws MalformedURLException, JOSEException {
        TokenGenerator tokenGenerator = createAdminToken();
        return restAssuredClient.executeGetRequestWithPaging(
                Optional.of(size),
                Optional.of(page),
                List.of(),
                getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listUuid + "/entries"),
                tokenGenerator.fetchTokenForRole());
    }

    public EntryGetSummaryDto findEntry(EntryPage page, UUID entryUuid) {
        return page.getContent().stream()
                .filter(item -> entryUuid.equals(item.getId()))
                .findFirst()
                .orElseThrow();
    }

    public void assertResultCodes(EntryGetSummaryDto dto, String... expectedCodes) {
        assertThat(dto.getIsResulted()).isTrue();
        assertEquals(expectedCodes.length, dto.getResulted().size());

        Set<String> codes =
                dto.getResulted().stream()
                        .map(ResultCodeGetSummaryDto::getResultCode)
                        .collect(Collectors.toSet());

        assertEquals(Set.of(expectedCodes), codes);
    }
}
