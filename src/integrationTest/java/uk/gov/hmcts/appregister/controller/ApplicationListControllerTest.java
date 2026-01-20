package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.http.HttpHeaders;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.testutils.client.PageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationListControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String GET_ENTRIES_CONTEXT = "application-list-entries";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    private static final String UNKNOWN_APPLICATION_LIST_ID =
            "ffffffff-ffff-ffff-ffff-ffffffffffff";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_COURT_CODE = "CCC003";
    private static final String VALID_COURT_NAME = "Cardiff Crown Court";
    private static final String VALID_COURT_CODE2 = "BCC006";

    private static final String VALID_CJA_CODE = "CD";
    private static final String VALID_CJA_CODE2 = "CE";

    private static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    private static final String UNKNOWN_COURT_CODE = "ZZZ999";
    private static final String UNKNOWN_CJA_CODE = "99X";

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    private static final LocalTime TEST_TIME = LocalTime.of(10, 30);

    private static final LocalDate TEST_DATE2 = LocalDate.of(2025, 10, 19);
    private static final LocalTime TEST_TIME2 = LocalTime.of(11, 30);

    @Autowired private ApplicationListEntryRepository aleRepository;

    // --- Happy path: create with COURT --------------------------------------------------------

    @Test
    void givenValidRequest_whenCreateWithCourt_then201AndBodyAndLocationHeader() throws Exception {
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

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());
        resp.then().contentType(VND_JSON_V1);

        // Location header should point to /application-lists/{uuid}
        String location = resp.getHeader("Location");
        assertThat(location).isNotBlank();
        assertThat(URI.create(location).getPath())
                .matches(".*/application-lists/[0-9a-fA-F\\-]{36}$");

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(0L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(TEST_DATE);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning_list_(court)");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE);
        assertThat(dto.getCourtName()).isEqualTo(VALID_COURT_NAME);
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();

        String eventName = "Create Application List";
        String operation = "CREATE";

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST, "id", "", null, operation, eventName));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_name",
                        null,
                        "Cardiff Crown Court",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        null,
                        VALID_COURT_CODE,
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        null,
                        req.getStatus().toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        "Morning_list_\\(court\\)",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        null,
                        TEST_TIME.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        null,
                        TEST_DATE.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "other_courthouse",
                        "",
                        null,
                        operation,
                        eventName));
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenCreateWithCja_then201AndBodyAndLocationHeader() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning_list_(cja)")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().contentType(VND_JSON_V1);

        String location = resp.getHeader("Location");
        assertThat(location).isNotBlank();
        assertThat(URI.create(location).getPath())
                .matches(".*/application-lists/[0-9a-fA-F\\-]{36}$");

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(0L);
        assertThat(dto.getDate()).isEqualTo(TEST_DATE);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME);
        assertThat(dto.getDescription()).isEqualTo("Morning_list_(cja)");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE);
        assertThat(dto.getOtherLocationDescription()).isEqualTo(VALID_OTHER_LOCATION);
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();

        String eventName = "Create Application List";
        String operation = "CREATE";

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "application_lists",
                        "application_list_status",
                        null,
                        req.getStatus().toString(),
                        operation,
                        eventName));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        "application_lists", "id", null, null, operation, eventName));

        differenceLogAsserter.assertFieldLogNotPresent(
                TableNames.APPICATION_LIST, "courthouse_code", true);

        differenceLogAsserter.assertFieldLogNotPresent(
                TableNames.APPICATION_LIST, "courthouse_name", true);

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        "Morning_list_\\(cja\\)",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "other_courthouse",
                        null,
                        "CJA_CD_DESCRIPTION",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        null,
                        TEST_TIME.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        null,
                        TEST_DATE.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        null,
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "other_courthouse",
                        "",
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        "",
                        "OPEN",
                        operation,
                        eventName));
    }

    // --- Validation: XOR rule (both supplied) -------------------------------------------------
    @Test
    void givenInvalidLocationCombination_whenCreate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Invalid XOR: both")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError
                        .INVALID_LOCATION_COMBINATION
                        .getCode(),
                resp);
    }

    // --- Validation: XOR rule (court supplied, cja supplied, other description not supplied ----
    @Test
    void givenInvalidLocationCombination_cjaIncludedMissingOtherDescription_whenCreate_then400()
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
                        .description("Invalid XOR: both")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(null);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError
                        .INVALID_LOCATION_COMBINATION
                        .getCode(),
                resp);
    }

    @Test
    void givenInvalidLocationCombination_cjaMissingOtherDescriptionIncluded_whenCreate_then400()
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
                        .description("Invalid XOR: both")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .cjaCode(null)
                        .otherLocationDescription(VALID_OTHER_LOCATION);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError
                        .INVALID_LOCATION_COMBINATION
                        .getCode(),
                resp);
    }

    // --- Not found: court ---------------------------------------------------------------------
    @Test
    void givenUnknownCourt_whenCreate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Unknown court")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(UNKNOWN_COURT_CODE);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(ApplicationListError.COURT_NOT_FOUND.getCode(), resp);
    }

    // --- Not found: CJA -----------------------------------------------------------------------
    @Test
    void givenUnknownCja_whenCreate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Unknown cja")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(UNKNOWN_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(ApplicationListError.CJA_NOT_FOUND.getCode(), resp);
    }

    @Test
    void givenNoRole_whenCreate_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("No role")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenClosedStatus_whenCreate_then400() throws Exception {
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
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError
                        .INVALID_NEW_LIST_STATUS
                        .getCode(),
                resp);
    }

    @Test
    void givenInvalidTime_whenCreate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var invalidTime = LocalTime.of(0, 0, 1);

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(invalidTime)
                        .description("list_(court)")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        // object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule timeModule = new JavaTimeModule();

        // ensure we serialize LocalTime with seconds
        timeModule.addSerializer(
                LocalTime.class,
                new JsonSerializer<LocalTime>() {
                    @Override
                    public void serialize(
                            LocalTime localTime,
                            JsonGenerator jsonGenerator,
                            SerializerProvider serializerProvider)
                            throws IOException {
                        jsonGenerator.writeString(localTime.toString());
                    }
                });
        objectMapper.registerModule(timeModule);

        String payloadWithSeconds = objectMapper.writeValueAsString(req);

        // do not internal serialise by passing a string
        Response resp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT), token, payloadWithSeconds);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        ProblemAssertUtil.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode(),
                "Text '00:00:01' " + "could not be parsed, unparsed text " + "found at index 5",
                resp);
    }

    @Test
    void givenInvalidTimeAsArray_whenCreate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var invalidTime = LocalTime.of(0, 0, 1);

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(invalidTime)
                        .description("list_(court)")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        // object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule timeModule = new JavaTimeModule();
        objectMapper.registerModule(timeModule);

        String payloadWithSeconds = objectMapper.writeValueAsString(req);

        // do not internal serialise by passing a string
        Response resp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT), token, payloadWithSeconds);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        ProblemAssertUtil.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode(),
                "JSON parse error: Unexpected " + "time format detected [0,0,1]",
                resp);
    }

    @Test
    void givenValidRequest_whenUpdateWithCourt_then200AndBody() throws Exception {
        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        courtLocationGetDetailDto.setName("Manchester Crown Court");
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning_list_(court)_update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(4)
                        .durationMinutes(32);

        String[] createdLocation = createAppListUsingRestApi();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // clear the logs before the update
        differenceLogAsserter.clearLogs();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());

        // Location header should point to /application-lists/{uuid}
        // Assert
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning_list_(court)_update");

        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);
        assertThat(dto.getDurationHours()).isEqualTo(4);
        assertThat(dto.getDurationMinutes()).isEqualTo(32);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE2);
        assertThat(dto.getCourtName()).isEqualTo("Bristol Crown Court");
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();

        String eventName = AppListAuditOperation.UPDATE_APP_LIST.getEventName();
        String operation = AppListAuditOperation.UPDATE_APP_LIST.getType().name();
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_name",
                        "Cardiff Crown Court",
                        "Bristol Crown Court",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        VALID_COURT_CODE,
                        VALID_COURT_CODE2,
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        ApplicationListStatus.OPEN.name(),
                        req.getStatus().toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        "Morning_list_\\(court\\)",
                        "Morning_list_\\(court\\)_update",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        TEST_TIME.toString(),
                        TEST_TIME2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        TEST_DATE.toString(),
                        TEST_DATE2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST, "version", "0", "1", operation, eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_hour",
                        "2",
                        "4",
                        operation,
                        eventName));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_minute",
                        "30",
                        "32",
                        operation,
                        eventName));
    }

    @Test
    void givenValidRequest_whenUpdateWithCourtNoChangesNoDiff_then200AndBody() throws Exception {

        String[] createdLocation = createAppListUsingRestApi();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning_list_\\(court\\)")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        // clear the logs before the update
        differenceLogAsserter.clearLogs();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void givenValidRequest_whenUpdateWithCourtWithMatchProblem_then412() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list \\(court\\) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(4)
                        .durationMinutes(32);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, "never going to match");

        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        ProblemAssertUtil.assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }

    @Test
    void givenValidRequest_whenUpdateWithCourtWithMatch_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list \\(court\\) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(4)
                        .durationMinutes(32);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, createdLocation[1]);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());

        // Location header should point to /application-lists/{uuid}
        // Assert
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning list \\(court\\) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);
        assertThat(dto.getDurationHours()).isEqualTo(4);
        assertThat(dto.getDurationMinutes()).isEqualTo(32);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE2);
        assertThat(dto.getCourtName()).isEqualTo("Bristol Crown Court");
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenUpdateWithCja_then201() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        differenceLogAsserter.clearLogs();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning_list_(court)_update")
                        .status(ApplicationListStatus.CLOSED)
                        .cjaCode(VALID_CJA_CODE2)
                        .durationHours(4)
                        .durationMinutes(32)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().contentType(VND_JSON_V1);

        // Assert
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L);
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2);
        assertThat(dto.getDescription()).isEqualTo("Morning_list_(court)_update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE2);
        assertThat(dto.getOtherLocationDescription()).isEqualTo("Updated other location");
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();

        String eventName = AppListAuditOperation.UPDATE_APP_LIST.getEventName();
        String operation = AppListAuditOperation.UPDATE_APP_LIST.getType().name();

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_name",
                        "Cardiff Crown Court",
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        VALID_COURT_CODE,
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        ApplicationListStatus.OPEN.name(),
                        req.getStatus().toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        "Morning_list_\\(court\\)",
                        "Morning_list_\\(court\\)_update",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        TEST_TIME.toString(),
                        TEST_TIME2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        TEST_DATE.toString(),
                        TEST_DATE2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST, "version", "0", "1", operation, eventName));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_hour",
                        "2",
                        "4",
                        operation,
                        eventName));

        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_minute",
                        "30",
                        "32",
                        operation,
                        eventName));
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenUpdateWithCjaWithMatch_then201() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        differenceLogAsserter.clearLogs();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list \\(court\\) update")
                        .status(ApplicationListStatus.CLOSED)
                        .cjaCode(VALID_CJA_CODE2)
                        .durationHours(4)
                        .durationMinutes(32)
                        .otherLocationDescription("Updated_other_location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, createdLocation[1]);

        resp.then().contentType(VND_JSON_V1);

        // Assert
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L);
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2);
        assertThat(dto.getDescription()).isEqualTo("Morning list \\(court\\) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE2);

        assertThat(dto.getOtherLocationDescription()).isEqualTo("Updated_other_location");
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();
    }

    @Test
    void givenValidRequest_whenUpdateWithCjaWithMatchProblem_then412() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list \\(court\\) update")
                        .status(ApplicationListStatus.CLOSED)
                        .cjaCode(VALID_CJA_CODE)
                        .durationHours(4)
                        .durationMinutes(32)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, "no match");
        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        ProblemAssertUtil.assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }

    // --- Validation: UUID not found for update -------------------------------------------------
    @Test
    void givenInvalidUUID_whenUpdate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list \\(court\\) update")
                        .status(ApplicationListStatus.CLOSED)
                        .cjaCode(VALID_CJA_CODE)
                        .durationHours(4)
                        .durationMinutes(32)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                ApplicationListError.APPLICATION_LIST_NOT_FOUND.getCode(), resp);
    }

    // --- Validation: XOR rule (both supplied) -------------------------------------------------
    @Test
    void givenInvalidLocationCombination_whenUpdate_then400() throws Exception {

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .cjaCode(VALID_CJA_CODE)
                        .durationHours(4)
                        .durationMinutes(32)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .otherLocationDescription("Updated other location");

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();
        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError
                        .INVALID_LOCATION_COMBINATION
                        .getCode(),
                resp);
    }

    // --- Not found: court ---------------------------------------------------------------------
    @Test
    void givenUnknownCourt_whenUpdate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .durationHours(4)
                        .durationMinutes(32)
                        .courtLocationCode("Unknown")
                        .otherLocationDescription(null);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(ApplicationListError.COURT_NOT_FOUND.getCode(), resp);
    }

    // --- Not found: CJA -----------------------------------------------------------------------
    @Test
    void givenUnknownCja_whenUpdate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .durationHours(4)
                        .durationMinutes(32)
                        .cjaCode("Unknown")
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(ApplicationListError.CJA_NOT_FOUND.getCode(), resp);
    }

    @Test
    @DisplayName("UPDATE: 404 when updating a soft-deleted list")
    void givenSoftDeleted_whenUpdate_then404() throws Exception {
        ApplicationListGetDetailDto created =
                createWithCourt("soft-deleted-update", TEST_DATE, TEST_TIME);
        UUID id = created.getId();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response deleteResp =
                restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        deleteResp.then().statusCode(HttpStatus.NO_CONTENT.value());

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Attempt update on soft-deleted")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(2)
                        .durationMinutes(15);

        Response resp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(
                ApplicationListError.APPLICATION_LIST_NOT_FOUND.getCode(), resp);
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        var validPayload =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("sec-matrix")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ApplicationListUpdateDto uploadPayload =
                Instancio.of(ApplicationListUpdateDto.class)
                        .withSettings(settings)
                        .ignore(field(ApplicationListUpdateDto::getCourtLocationCode))

                        // Instancio does not honour Max and Min annotations
                        .ignore(field(ApplicationListUpdateDto::getDurationHours))
                        .ignore(field(ApplicationListUpdateDto::getDurationMinutes))
                        .create();

        uploadPayload.setDurationHours(1);
        uploadPayload.setDurationMinutes(1);

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
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.USER)
                        .build());
    }

    /**
     * A utility method to create a new record.
     *
     * @return A two part array:- [0] - the uuid that can be used to fetch or update the record [1]-
     *     the etag for optimistic locking at the api level
     */
    private String[] createAppListUsingRestApi() throws Exception {
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

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        return new String[] {resp.header(HttpHeaders.LOCATION), resp.header(HttpHeaders.ETAG)};
    }

    @Test
    void givenValidRequest_whenDeleteWithValidId_then204() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning list (cja)")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for deletion
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        differenceLogAsserter.clearLogs();

        // fire tests
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(2, false);
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "al_id",
                        null,
                        null,
                        "DELETE",
                        "Delete Application List"));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "version",
                        "0",
                        null,
                        "DELETE",
                        "Delete Application List"));

        // assert success
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void givenValidRequest_whenDeleteWithInvalidId_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        // fire tests
        Response resp =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.DELETION_ID_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(0, true);
    }

    @Test
    void givenValidRequest_whenDeleteTwice_thenSecondDeleteReturns404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning list (cja)")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for deletion
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        differenceLogAsserter.clearLogs();

        // prove the delete has been made
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        resp.then().statusCode(HttpStatus.CONFLICT.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode().getAppCode(),
                problemDetail.getType().toString());

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(0, true);
    }

    // --- GET_ALL ---------------------------------------------------------------------
    private static String uniquePrefix(String base) {
        return base + " :: " + UUID.randomUUID();
    }

    private static PageMetaData stdPageMeta() {
        return new PageMetaData() {
            @Override
            public String getPageNumberQueryName() {
                return "page";
            }

            @Override
            public String getPageSizeQueryName() {
                return "size";
            }

            @Override
            public String getSortName() {
                return "sort";
            }
        };
    }

    private ApplicationListGetDetailDto createWithCourt(
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

    private ApplicationListGetDetailDto createWithCja(
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

    @Test
    @DisplayName("GET: 403 when no role")
    void givenNoRole_whenGet_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1),
                        null);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("GET: default paging + default sort (description ASC)")
    void givenDefaults_whenGet_then200AndSortedByDescriptionAsc() throws Exception {

        String prefix = uniquePrefix("get-default-sort");

        createWithCourt(prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCourt(prefix + " - Alpha", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCourt(prefix + " - Mango", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(), // Rely on default sort
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getDescription()).endsWith("Alpha");
        assertThat(page.getContent().get(1).getDescription()).endsWith("Mango");
        assertThat(page.getContent().get(2).getDescription()).endsWith("Zebra");

        assertThat(page.getPageNumber()).isZero();
        assertThat(page.getPageSize()).isGreaterThanOrEqualTo(3);
        assertThat(page.getFirst()).isTrue();
    }

    @Test
    @DisplayName("GET: paging works (page=1,size=2)")
    void givenPaging_whenSecondPage_thenCorrectMetadata() throws Exception {

        String prefix = uniquePrefix("get-paging");

        createWithCourt(prefix + " - A", LocalDate.of(2025, 10, 14), LocalTime.of(9, 0));
        createWithCourt(prefix + " - B", LocalDate.of(2025, 10, 15), LocalTime.of(9, 0));
        createWithCourt(prefix + " - C", LocalDate.of(2025, 10, 16), LocalTime.of(9, 0));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(2),
                        Optional.of(1),
                        List.of(), // default sort (description ASC)
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        stdPageMeta());

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getPageNumber()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getElementsOnPage()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET: filter by date + time (exact match)")
    void givenDateAndTimeFilter_thenOnlyThatSlot() throws Exception {

        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t0930 = LocalTime.of(9, 30);
        LocalTime t1030 = LocalTime.of(10, 30);

        createWithCourt(prefix + " - keep", day, t0930);
        createWithCourt(prefix + " - drop-1", day, t1030);
        createWithCourt(prefix + " - drop-2", day.plusDays(1), t0930);

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("description", prefix)
                                        .queryParam("date", day.toString()) // yyyy-MM-dd
                                        .queryParam("time", "09:30"),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getDate()).isEqualTo(day);
        assertThat(only.getTime()).isEqualTo(t0930);
        assertThat(only.getDescription()).endsWith("keep");
    }

    @Test
    @DisplayName("GET: filter by 23:59")
    void givenTimeFilter_thenSlot() throws Exception {

        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t2359 = LocalTime.of(23, 59);

        createWithCourt(prefix + " - keep", day, t2359);

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("time", "23:59"),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getTime()).isEqualTo(t2359);
    }

    @Test
    @DisplayName("GET: filter by courtLocationCode")
    void givenCourtFilter_thenOnlyCourtRows() throws Exception {

        String prefix = uniquePrefix("get-court-filter");

        createWithCourt(prefix + " - court", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCja(prefix + " - cja", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("description", prefix)
                                        .queryParam("courtLocationCode", VALID_COURT_CODE),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getLocation()).isEqualTo(VALID_COURT_NAME);
    }

    @Test
    @DisplayName("GET: filter by cjaCode")
    void givenCjaFilter_thenOnlyCjaRows() throws Exception {

        String prefix = uniquePrefix("get-cja-filter");

        createWithCja(prefix + " - cja", LocalDate.of(2025, 10, 16), LocalTime.of(11, 0));
        createWithCourt(prefix + " - court", LocalDate.of(2025, 10, 16), LocalTime.of(11, 0));

        var adminToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        adminToken,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("description", prefix)
                                        .queryParam("cjaCode", VALID_CJA_CODE),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getDescription()).contains(prefix);
    }

    @Test
    @DisplayName("GET: does not return soft deleted list")
    void givenDefaults_whenGet_then200AndNoSoftDeletedSlot() throws Exception {

        // setup a record for deletion
        String prefix = uniquePrefix("soft-deleted");
        ApplicationListGetDetailDto dto =
                createWithCourt(
                        prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        UUID id = dto.getId();

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), userToken);
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(), // Rely on default sort
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(0);
    }

    @Test
    @DisplayName("GET: allowed sort (date,desc & time,desc)")
    void givenAllowedSort_thenSorted() throws Exception {

        String prefix = uniquePrefix("get-sort-allowed");

        createWithCourt(prefix + " - A", LocalDate.of(2025, 10, 14), LocalTime.of(9, 0));
        createWithCourt(prefix + " - B", LocalDate.of(2025, 10, 15), LocalTime.of(10, 0));
        createWithCourt(prefix + " - C", LocalDate.of(2025, 10, 15), LocalTime.of(9, 0));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("date,desc", "time,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getDescription()).endsWith("B");
        assertThat(page.getContent().get(1).getDescription()).endsWith("C");
        assertThat(page.getContent().get(2).getDescription()).endsWith("A");
    }

    @Test
    @DisplayName("GET: disallowed sort (cja) -> 400")
    void givenDisallowedSort_then400() throws Exception {

        String prefix = uniquePrefix("get-sort-disallowed");

        createWithCourt(prefix + " - X", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("cja,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("GET page: entriesCount excludes soft-deleted entry")
    void givenEntryDeleted_whenGetApplicationLists_thenEntriesCountExcludesDeleted()
            throws Exception {

        // 1) Create application list via API
        String prefix = uniquePrefix("entries-delete");
        ApplicationListGetDetailDto created =
                createWithCourt(prefix + " - list", TEST_DATE, TEST_TIME);
        UUID listId = created.getId();

        // 2) Prepare token to create entries
        var token = getToken();

        // 3) Build two EntryCreateDto payloads
        EntryCreateDto entryCreateDto1 = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        EntryCreateDto entryCreateDto2 = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        // 4) Create entries
        Response createResp1 =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"),
                        token,
                        entryCreateDto1);
        createResp1.then().statusCode(HttpStatus.CREATED.value());
        EntryGetDetailDto createdEntry1 = createResp1.as(EntryGetDetailDto.class);

        Response createResp2 =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"),
                        token,
                        entryCreateDto2);
        createResp2.then().statusCode(HttpStatus.CREATED.value());
        EntryGetDetailDto createdEntry2 = createResp2.as(EntryGetDetailDto.class);

        // 5) Call the entries search endpoint to fetch entries for this list
        Response entriesPageResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(20),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(GET_ENTRIES_CONTEXT),
                        token);

        entriesPageResp.then().statusCode(HttpStatus.OK.value());

        EntryPage entriesPage = entriesPageResp.as(EntryPage.class);
        assertThat(entriesPage.getContent()).isNotNull();

        boolean foundCreated2 =
                entriesPage.getContent().stream()
                        .anyMatch(e -> createdEntry2.getId().equals(e.getId()));
        assertThat(foundCreated2)
                .withFailMessage("createdEntry2 must be present in entries search results")
                .isTrue();

        UUID idFromSearch =
                entriesPage.getContent().stream()
                        .map(EntryGetSummaryDto::getId)
                        .filter(id -> createdEntry2.getId().equals(id))
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("Entry id not found by entries search"));

        // 6) soft-delete the entry
        aleRepository.softDeleteByUuid(idFromSearch);
        aleRepository.flush(); // ensure DB is updated for subsequent controller query

        // 7) Call the GET /application-lists endpoint
        Response pageResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl("application-lists"),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        // 8) Assert that entriesCount excludes the deleted entry
        pageResp.then().statusCode(HttpStatus.OK.value());
        ApplicationListPage page = pageResp.as(ApplicationListPage.class);

        var list = page.getContent().getFirst();
        assertThat(list.getEntriesCount())
                .withFailMessage("entriesCount should exclude the deleted entry removed via repo")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("GET Application List")
    void givenValidRequest_whenGetApplicationList_then200AndBody() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire test
        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);

        dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getDescription()).isEqualToIgnoringCase(description);
        assertThat(dto.getCjaCode()).isEqualToIgnoringCase(VALID_CJA_CODE);
        assertThat(dto.getEntriesCount()).isEqualTo(0);
        assertThat(dto.getEntriesSummary()).isNotNull();
    }

    @Test
    @DisplayName("GET Application List")
    void givenValidRequest_whenGetApplicationList_then400IdFormatting() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);

        // fire test
        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/232322"), token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(CommonAppError.TYPE_MISMATCH_ERROR.getCode().getAppCode());
        assertThat(problemDetail.getDetail())
                .contains("Problem with value 232322 for parameter listId");
        assertThat(problemDetail.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("GET Application List: 403 when no role")
    void givenNoRole_whenGetApplicationList_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        UUID id = UUID.randomUUID();

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    // --- Not found: Application List -------------------------------------------------
    @Test
    @DisplayName("GET Application List: 404 when list unknown")
    void givenUnknownApplicationList_whenGetApplicationList_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        UUID id = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("GET Application List: 404 when list soft deleted")
    void givenSoftDeletedApplicationList_whenGetApplicationList_then404() throws Exception {
        ApplicationListGetDetailDto created =
                createWithCourt("soft-deleted-get", TEST_DATE, TEST_TIME);
        UUID id = created.getId();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response deleteResp =
                restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        deleteResp.then().statusCode(HttpStatus.NO_CONTENT.value());

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName(
            "GET Application List: entriesSummary and entriesCount exclude soft-deleted entries")
    void givenEntrySoftDeleted_whenGetApplicationList_thenDeletedEntryExcludedFromSummaryAndCount()
            throws Exception {

        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("get-by-id-exclude-deleted"));

        // create two entries
        final EntryGetDetailDto entry1 = createEntry(listId);
        final EntryGetDetailDto entry2 = createEntry(listId);

        // sanity-check that initial GET shows two entries
        ApplicationListGetDetailDto initial = getApplicationListDetail(listId, token);
        assertThat(initial.getEntriesCount()).isEqualTo(2L);
        assertThat(initial.getEntriesSummary()).isNotNull();
        assertThat(initial.getEntriesSummary().size()).isGreaterThanOrEqualTo(2);

        // soft-delete 2nd entry
        softDeleteEntry(entry2.getId());

        // GET again and assert
        ApplicationListGetDetailDto after = getApplicationListDetail(listId, token);
        assertThat(after.getEntriesCount())
                .withFailMessage("entriesCount should exclude the soft-deleted entry")
                .isEqualTo(1L);

        assertThat(after.getEntriesSummary())
                .withFailMessage("entriesSummary must be present")
                .isNotNull();

        List<UUID> returnedEntryIds =
                after.getEntriesSummary().stream()
                        .map(ApplicationListEntrySummary::getUuid)
                        .toList();

        assertThat(returnedEntryIds)
                .withFailMessage("Soft-deleted entry must not appear in entriesSummary")
                .doesNotContain(entry2.getId());

        // sanity: remaining entry should be the first created one
        assertThat(returnedEntryIds).contains(entry1.getId());
    }

    @Test
    @DisplayName("Print Application List")
    void givenValidRequest_whenPrintApplicationList_then200AndBody() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire test
        Response printApplicationListResp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        printApplicationListResp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);

        ApplicationListGetPrintDto applicationListGetPrintDto =
                printApplicationListResp.as(ApplicationListGetPrintDto.class);
        assertThat(applicationListGetPrintDto.getEntries()).isNotNull();
    }

    @Test
    @DisplayName("Print Application List: 403 when no role")
    void givenNoRole_whenPrintApplicationList_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        UUID id = UUID.randomUUID();

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    // --- Not found: Application List -------------------------------------------------
    @Test
    @DisplayName("Print Application List: 404 when list unknown")
    void givenUnknownApplicationList_whenPrintApplicationList_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        UUID id = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Print Application List: entries exclude soft-deleted entries")
    void givenEntrySoftDeleted_whenPrintApplicationList_thenDeletedEntryExcludedFromPrint()
            throws Exception {

        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("print-exclude-deleted"));

        // create two entries
        final EntryGetDetailDto entry1 = createEntry(listId);
        final EntryGetDetailDto entry2 = createEntry(listId);

        // soft-delete 2nd entry
        softDeleteEntry(entry2.getId());

        // call print endpoint
        ApplicationListGetPrintDto printDto = getApplicationListPrint(listId, token);

        assertThat(printDto.getEntries())
                .withFailMessage("entries in print output must not be null")
                .isNotNull();

        List<UUID> returnedEntryIds =
                printDto.getEntries().stream().map(EntryGetPrintDto::getId).toList();

        assertThat(returnedEntryIds)
                .withFailMessage("Soft-deleted entry must not appear in print entries")
                .doesNotContain(entry2.getId());

        // sanity: remaining printed entry should include the first created one
        assertThat(returnedEntryIds).contains(entry1.getId());
    }

    private UUID createApplicationList(TokenAndJwksKey token, String prefix) throws Exception {
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

    private EntryGetDetailDto createEntry(UUID listId) throws Exception {
        var entryDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        Response createEntryResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"), getToken(), entryDto);
        createEntryResp.then().statusCode(HttpStatus.CREATED.value());

        return createEntryResp.as(EntryGetDetailDto.class);
    }

    private void softDeleteEntry(UUID entryId) {
        aleRepository.softDeleteByUuid(entryId);
        aleRepository.flush();
    }

    private ApplicationListGetDetailDto getApplicationListDetail(UUID listId, TokenAndJwksKey token)
            throws Exception {
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1));
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    private ApplicationListGetPrintDto getApplicationListPrint(UUID listId, TokenAndJwksKey token)
            throws Exception {
        Response resp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/print"), token);
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetPrintDto.class);
    }
}
