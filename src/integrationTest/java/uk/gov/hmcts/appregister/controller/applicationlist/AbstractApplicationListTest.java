package uk.gov.hmcts.appregister.controller.applicationlist;

import static org.instancio.Select.field;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.http.HttpHeaders;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.controller.ApplicationEntryResultControllerTest;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public abstract class AbstractApplicationListTest extends AbstractSecurityControllerTest {

    protected static final String WEB_CONTEXT = "application-lists";
    protected static final String GET_ENTRIES_CONTEXT = "application-list-entries";
    protected static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    protected static final String UNKNOWN_APPLICATION_LIST_ID =
            "ffffffff-ffff-ffff-ffff-ffffffffffff";

    // --- Seeded reference data ----------------------------------------------------
    protected static final String VALID_COURT_CODE = "CCC003";
    protected static final String VALID_COURT_NAME = "Cardiff Crown Court";
    protected static final String VALID_COURT_CODE2 = "BCC006";

    protected static final String VALID_CJA_CODE = "CD";
    protected static final String VALID_CJA_CODE2 = "CE";

    protected static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    protected static final String UNKNOWN_COURT_CODE = "ZZZ999";
    protected static final String UNKNOWN_CJA_CODE = "99";

    protected static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    protected static final LocalTime TEST_TIME = LocalTime.of(10, 30);

    protected static final LocalDate TEST_DATE2 = LocalDate.of(2025, 10, 19);
    protected static final LocalTime TEST_TIME2 = LocalTime.of(11, 30);

    @Autowired protected ApplicationListRepository applicationListRepository;

    @Autowired protected TransactionalUnitOfWork unitOfWork;

    @Autowired protected ApplicationListEntryRepository aleRepository;

    /**
     * A utility method to create a new record.
     *
     * @return A two part array:- [0] - the uuid that can be used to fetch or update the record [1]-
     *     the etag for optimistic locking at the api level
     */
    protected String[] createAppListUsingRestApi() throws Exception {
        return createAppListUsingRestApi(null);
    }

    /**
     * A utility method to create a new record.
     *
     * @param modifyCallback A callback that allows to modify the payload.
     * @return A two part array:- [0] - the uuid that can be used to fetch or update the record [1]-
     *     the etag for optimistic locking at the api level
     */
    protected String[] createAppListUsingRestApi(Consumer<ApplicationListCreateDto> modifyCallback)
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

    protected EntryGetDetailDto createEntry(UUID listId) throws Exception {
        return createEntry(listId, null);
    }

    protected EntryGetDetailDto createEntry(UUID listId, Consumer<EntryCreateDto> modifyCallback)
            throws Exception {
        var entryDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        if (modifyCallback != null) {
            modifyCallback.accept(entryDto);
        }

        Response createEntryResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"), getToken(), entryDto);
        createEntryResp.then().statusCode(HttpStatus.CREATED.value());

        return createEntryResp.as(EntryGetDetailDto.class);
    }

    protected EntryGetDetailDto createEntryForClose(UUID listId) throws Exception {
        var entryDto = CreateEntryDtoUtil.getCorrectCreateEntryDto(true);

        Response createEntryResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"), getToken(), entryDto);
        createEntryResp.then().statusCode(HttpStatus.CREATED.value());

        return createEntryResp.as(EntryGetDetailDto.class);
    }

    protected void createResultSuccess(UUID listId, UUID entryId) throws Exception {
        var payload =
                ApplicationEntryResultControllerTest.buildCreatePayload(
                        ApplicationEntryResultControllerTest.APPC_CODE,
                        List.of(
                                new TemplateSubstitution(
                                        ApplicationEntryResultControllerTest.WORDING_KEY,
                                        "The Central Criminal Court")));

        Response createEntryResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results"),
                        getToken(),
                        payload);
        createEntryResp.then().statusCode(HttpStatus.CREATED.value());
    }

    /**
     * gets the first open list.
     *
     * @return The uuid of the first open list
     */
    protected UUID getFirstOpenListToUpdate() {
        ApplicationList applicationList =
                unitOfWork.inTransaction(
                        () -> {
                            return applicationListRepository
                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                    .getFirst();
                        });
        return applicationList.getUuid();
    }

    protected ApplicationListGetDetailDto createWithCourt(
            String description, LocalDate date, LocalTime time) throws Exception {

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(date)
                        .time(time)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    protected ApplicationListGetDetailDto createWithCja(
            String description, LocalDate date, LocalTime time) throws Exception {

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(date)
                        .time(time)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    protected UUID createApplicationList(TokenAndJwksKey token, String prefix) throws Exception {
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(prefix + " - list")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        Response createListResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT), token, createListReq);
        createListResp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto createdList =
                createListResp.as(ApplicationListGetDetailDto.class);
        return createdList.getId();
    }

    protected void softDeleteEntry(UUID entryId) {
        aleRepository.softDeleteByUuid(entryId);
        aleRepository.flush();
    }

    protected ApplicationListGetDetailDto getApplicationListDetail(
            UUID listId, TokenAndJwksKey token) throws Exception {
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1));
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    protected ApplicationListGetPrintDto getApplicationListPrint(UUID listId, TokenAndJwksKey token)
            throws Exception {
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/print"), token);
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetPrintDto.class);
    }

    // --- GET_ALL ---------------------------------------------------------------------
    protected static String uniquePrefix(String base) {
        return base + " :: " + UUID.randomUUID();
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ApplicationListUpdateDto uploadPayload =
                Instancio.of(ApplicationListUpdateDto.class)
                        .withSettings(settings)
                        .ignore(field(ApplicationListUpdateDto::getCourtLocationCode))

                        // Instancio does not honour Max and Min annotations
                        .ignore(field(ApplicationListUpdateDto::getDurationHours))
                        .ignore(field(ApplicationListUpdateDto::getDurationMinutes))
                        .create();

        uploadPayload.setCjaCode(null);
        uploadPayload.setDurationHours(1);
        uploadPayload.setDurationMinutes(1);

        var validPayload =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("sec-matrix")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(uploadPayload)
                        .successRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(uploadPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
