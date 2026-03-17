package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.controller.applicationentry.AbstractApplicationEntryCrudTest;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public abstract class AbstractApplicationEntryResultCrudTest extends BaseIntegration {

    @MockitoBean protected UserProvider provider;

    @Autowired protected AppListEntryResolutionRepository appListEntryResolutionRepository;
    @Autowired protected ResolutionCodeRepository resolutionCodeRepository;
    @Autowired protected TransactionalUnitOfWork unitOfWork;
    @Autowired protected ApplicationListRepository applicationListRepository;
    @Autowired protected ApplicationListEntryRepository applicationListEntryRepository;

    public static final String WEB_CONTEXT = "application-lists";

    public static final String APPC_CODE = "APPC";
    public static final String APPC_WORDING_KEY = "Name of Crown Court";

    public static final String FRO_CODE = "FRO";
    public static final String FRO_WORDING_KEY = "Reason text";

    protected static final String CREATE_ENTRY_CONTEXT = "application-lists";

    protected static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    protected static final LocalTime TEST_TIME = LocalTime.of(10, 30);

    protected static final String VALID_COURT_CODE = "CCC003";

    @BeforeEach
    void setupUser() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

    // ---- HTTP helpers ----
    protected Response deleteResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token)
            throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
                getLocalUrl(
                        WEB_CONTEXT
                                + "/"
                                + listId
                                + "/entries/"
                                + entryId
                                + "/results/"
                                + resultId),
                token);
    }

    protected Response deleteResult(
            UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token, String ifMatch)
            throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
                getLocalUrl(
                        WEB_CONTEXT
                                + "/"
                                + listId
                                + "/entries/"
                                + entryId
                                + "/results/"
                                + resultId),
                token,
                ifMatch);
    }

    protected Response createResult(UUID listId, UUID entryId, TokenAndJwksKey token, Object body)
            throws MalformedURLException {
        return restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results"),
                token,
                body);
    }

    protected Response updateResult(
            UUID listId,
            UUID entryId,
            UUID resultId,
            TokenAndJwksKey token,
            Object body,
            String ifMatch)
            throws MalformedURLException {
        return restAssuredClient.executePutRequest(
                getLocalUrl(
                        WEB_CONTEXT
                                + "/"
                                + listId
                                + "/entries/"
                                + entryId
                                + "/results/"
                                + resultId),
                token,
                body,
                ifMatch);
    }

    // ---- data helpers ----
    public ApplicationList createAndSaveList(Status status) {
        var list = new AppListTestData().someMinimal().status(status).build();
        persistance.save(list);
        return list;
    }

    AbstractApplicationEntryCrudTest.SuccessCreateEntryResponse createEntry(UUID applicationListId)
            throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now().toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + applicationListId + "/entries"),
                        token,
                        entryCreateDto);

        responseSpecCreate.then().statusCode(201);
        Assertions.assertNotNull(HeaderUtil.getETag(responseSpecCreate));

        return new AbstractApplicationEntryCrudTest.SuccessCreateEntryResponse(
                responseSpecCreate.as(EntryGetDetailDto.class), responseSpecCreate);
    }

    protected ApplicationListEntry createEntry(ApplicationList list) {
        return new AppListEntryTestData().someMinimal().applicationList(list).build();
    }

    protected AppListEntryResolution createAndSaveResolution(
            ApplicationListEntry entry, ResolutionCode resolutionCode) {
        var resolution =
                new AppListEntryResolutionTestData()
                        .someMinimal()
                        .resolutionWording(AppListEntryResolutionTestData.WORDING_1)
                        .applicationList(entry)
                        .resolutionCode(resolutionCode)
                        .build();
        return persistance.save(resolution);
    }

    public static ResultCreateDto buildCreatePayload(
            String resultCode, List<TemplateSubstitution> wordingFields) {
        return new ResultCreateDto(resultCode, wordingFields);
    }

    public static ResultUpdateDto buildUpdatePayload(
            String resultCode, List<TemplateSubstitution> wordingFields) {
        return new ResultUpdateDto(resultCode, wordingFields);
    }

    protected ResolutionCode saveActiveResolutionCode(String code, LocalDate start, LocalDate end) {
        ResolutionCode rc = new ResolutionCodeTestData().someComplete();
        rc.setResultCode(code);
        rc.setStartDate(start);
        rc.setEndDate(end);
        return persistance.save(rc);
    }

    // ---- context helpers ----

    protected record ExistingEntryContext(
            ApplicationList list, ApplicationListEntry entry, TokenAndJwksKey token) {}

    protected record ExistingEntryResultContext(
            ApplicationList list,
            ApplicationListEntry entry,
            AppListEntryResolution entryResult,
            TokenAndJwksKey token) {}

    protected ExistingEntryContext givenExistingEntry() throws Exception {
        var list = createAndSaveList(Status.OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var token = getToken();
        return new ExistingEntryContext(list, entry, token);
    }

    protected ExistingEntryResultContext givenExistingEntryResult() throws Exception {
        return givenExistingEntryResult(Status.OPEN);
    }

    protected ExistingEntryResultContext givenExistingEntryResult(Status status) throws Exception {
        var list = createAndSaveList(status);
        var entry = createEntry(list);
        persistance.save(entry);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        resolutionCode.setResultCode(APPC_CODE);
        persistance.save(resolutionCode);

        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();
        return new ExistingEntryResultContext(list, entry, entryResult, token);
    }

    /**
     * Gets an entry result.
     *
     * @param appListId The application list id
     * @param entryId The entry id
     * @param pageSize The page size
     * @param pageNumber The page number
     * @return A response containing the created entry result and the raw HTTP response
     */
    protected Response getEntryResult(
            TokenAndJwksKey token,
            UUID appListId,
            UUID entryId,
            Integer pageSize,
            Integer pageNumber)
            throws Exception {
        return restAssuredClient.executeGetRequestWithPaging(
                Optional.of(pageSize),
                Optional.of(pageNumber),
                List.of(),
                getLocalUrl(
                        CREATE_ENTRY_CONTEXT
                                + "/"
                                + appListId
                                + "/entries/"
                                + entryId
                                + "/results"),
                token);
    }

    /**
     * A utility method to create a new record.
     *
     * @param modifyCallback A callback that allows to modify the payload.
     * @return A two part array:- [0] - the uuid that can be used to fetch or update the record [1]-
     *     the etag for optimistic locking at the api level
     */
    protected String[] createAppList(Consumer<ApplicationListCreateDto> modifyCallback)
            throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning_list_(court)")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        if (modifyCallback != null) {
            modifyCallback.accept(req);
        }

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        return new String[] {resp.header(HttpHeaders.LOCATION), resp.header(HttpHeaders.ETAG)};
    }
}
