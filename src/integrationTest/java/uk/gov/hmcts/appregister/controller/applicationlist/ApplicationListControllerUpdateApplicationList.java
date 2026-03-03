package uk.gov.hmcts.appregister.controller.applicationlist;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationListControllerUpdateApplicationList extends AbstractApplicationListTest {

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
        assertThat(dto.getEntriesSummary()).isNotNull();

        String eventName = "Create Application List";
        String operation = "CREATE";

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST, "id", "", null, operation, eventName));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_name",
                        null,
                        "Cardiff Crown Court",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        null,
                        VALID_COURT_CODE,
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        null,
                        req.getStatus().toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        "Morning_list_\\(court\\)",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        null,
                        TEST_TIME.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        null,
                        TEST_DATE.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
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
        assertThat(dto.getEntriesSummary()).isNotNull();

        String eventName = "Create Application List";
        String operation = "CREATE";

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        "application_lists",
                        "application_list_status",
                        null,
                        req.getStatus().toString(),
                        operation,
                        eventName));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        "application_lists", "id", null, null, operation, eventName));

        differenceLogAsserter.assertFieldLogNotPresent(
                TableNames.APPICATION_LIST, "courthouse_code", true);

        differenceLogAsserter.assertFieldLogNotPresent(
                TableNames.APPICATION_LIST, "courthouse_name", true);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        null,
                        "Morning_list_\\(cja\\)",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "other_courthouse",
                        null,
                        "CJA_CD_DESCRIPTION",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        null,
                        TEST_TIME.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        null,
                        TEST_DATE.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.CRIMINAL_JUSTICE_AREA,
                        "cja_id",
                        null,
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "other_courthouse",
                        "",
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
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
                "Type conversion problem. Something in the payload is not correct",
                resp);
    }

    @Test
    void givenValidRequest_whenUpdateWithCourt_then200AndBody() throws Exception {
        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        courtLocationGetDetailDto.setName("Manchester Crown Court");

        String[] createdLocation = createAppListUsingRestApi();

        String listId = HeaderUtil.getTrailingIdFromLocation(createdLocation[0]);

        // add 2 entries
        createEntry(UUID.fromString(listId));
        createEntry(UUID.fromString(listId));

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        // clear the logs before the update
        differenceLogAsserter.clearLogs();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning_list_(court)_update")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(4)
                        .durationMinutes(32);

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
        assertThat(dto.getEntriesCount()).isEqualTo(2);

        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
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
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_name",
                        "Cardiff Crown Court",
                        "Bristol Crown Court",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        VALID_COURT_CODE,
                        VALID_COURT_CODE2,
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        ApplicationListStatus.OPEN.name(),
                        req.getStatus().toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        "Morning_list_\\(court\\)",
                        "Morning_list_\\(court\\)_update",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        TEST_TIME.toString(),
                        TEST_TIME2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        TEST_DATE.toString(),
                        TEST_DATE2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST, "version", "0", "1", operation, eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_hour",
                        "2",
                        "4",
                        operation,
                        eventName));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
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

    @Test
    void givenValidRequest_whenUpdateWithCourtWithReturnedEntrySummaries_then200AndBody()
            throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        courtLocationGetDetailDto.setName("Manchester Crown Court");
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(4)
                        .durationMinutes(32);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(
                                        getLocalUrl(WEB_CONTEXT)
                                                + "/"
                                                + getFirstOpenListToUpdate().toString())
                                .toURL(),
                        token,
                        req);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());

        // Location header should point to /application-lists/{uuid}
        // Assert
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(2L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(LocalDate.parse("2025-10-19"));
        assertThat(dto.getTime()).isEqualTo("11:30"); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning list (court) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(dto.getDurationHours()).isEqualTo(4);
        assertThat(dto.getDurationMinutes()).isEqualTo(32);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE2);
        assertThat(dto.getCourtName()).isEqualTo("Bristol Crown Court");
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();
        assertThat(dto.getEntriesSummary()).hasSize(5);
        assertThat(dto.getEntriesSummary().get(0).getApplicationTitle())
                .isEqualTo("Copy documents");
        assertThat(dto.getEntriesSummary().get(0).getResult().get()).isEqualTo("APPC");
        assertThat(dto.getEntriesSummary().get(1).getApplicationTitle())
                .isEqualTo("Copy documents");
        assertThat(dto.getEntriesSummary().get(1).getResult().get()).isEqualTo("AUTH");
        assertThat(dto.getEntriesSummary().get(2).getApplicationTitle())
                .isEqualTo("Copy documents (electronic)");
        assertThat(dto.getEntriesSummary().get(3).getApplicationTitle())
                .isEqualTo("Extract from the Court Register");
        assertThat(dto.getEntriesSummary().get(4).getApplicationTitle())
                .isEqualTo("Certificate of Satisfaction");
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenUpdateWithCja_then201() throws Exception {
        String[] createdLocation = createAppListUsingRestApi();

        differenceLogAsserter.clearLogs();

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning_list_(court)_update")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE2)
                        .durationHours(4)
                        .durationMinutes(32)
                        .otherLocationDescription("Updated other location");

        String listId = HeaderUtil.getTrailingIdFromLocation(createdLocation[0]);

        // add 2 entries
        createEntry(UUID.fromString(listId));
        createEntry(UUID.fromString(listId));

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

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
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(dto.getEntriesCount()).isEqualTo(2);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE2);
        assertThat(dto.getOtherLocationDescription()).isEqualTo("Updated other location");
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();

        String eventName = AppListAuditOperation.UPDATE_APP_LIST.getEventName();
        String operation = AppListAuditOperation.UPDATE_APP_LIST.getType().name();

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_name",
                        "Cardiff Crown Court",
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "courthouse_code",
                        VALID_COURT_CODE,
                        "",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_status",
                        ApplicationListStatus.OPEN.name(),
                        req.getStatus().toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "list_description",
                        "Morning_list_\\(court\\)",
                        "Morning_list_\\(court\\)_update",
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_time",
                        TEST_TIME.toString(),
                        TEST_TIME2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "application_list_date",
                        TEST_DATE.toString(),
                        TEST_DATE2.toString(),
                        operation,
                        eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST, "version", "0", "1", operation, eventName));
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_hour",
                        "2",
                        "4",
                        operation,
                        eventName));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "duration_minute",
                        "30",
                        "32",
                        operation,
                        eventName));
    }

    @Test
    public void givenValidRequest_whenUpdateForClose_then200() throws Exception {
        String[] createdLocation = createAppListUsingRestApi();

        // create an entry
        EntryGetDetailDto entryGetSummaryDto =
                createEntryForClose(
                        UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createdLocation[0])));

        // create the result for the entry
        createResultSuccess(entryGetSummaryDto.getListId(), entryGetSummaryDto.getId());

        // close the app list
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(4)
                        .durationMinutes(32);

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, createdLocation[1]);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    public void givenInvalidRequestNoDuration_whenUpdateForClose_then400() throws Exception {
        String[] createdLocation =
                createAppListUsingRestApi((dto) -> dto.durationHours(0).durationMinutes(0));

        // create an entry
        EntryGetDetailDto entryGetSummaryDto =
                createEntryForClose(
                        UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createdLocation[0])));

        // create the result for the entry
        createResultSuccess(entryGetSummaryDto.getListId(), entryGetSummaryDto.getId());

        // close the app list
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2);

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);
        ProblemAssertUtil.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_DURATION.getCode(), resp);
    }

    @Test
    public void givenInvalidRequestNoResultEntries_whenUpdateForClose_then400() throws Exception {
        String[] createdLocation = createAppListUsingRestApi();

        // create an entry
        createEntry(UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createdLocation[0])));

        // close the app list
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2);
        req.setDurationMinutes(20);

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);
        ProblemAssertUtil.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_NOT_RESULTED.getCode(), resp);
    }

    @Test
    public void givenInvalidRequestNoOfficials_whenUpdateForClose_then400() throws Exception {
        String[] createdLocation = createAppListUsingRestApi();

        // create an entry
        EntryGetDetailDto entryGetSummaryDto =
                createEntry(
                        UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createdLocation[0])),
                        (dto) -> dto.setOfficials(List.of()));

        // create the result for the entry
        createResultSuccess(entryGetSummaryDto.getListId(), entryGetSummaryDto.getId());

        // close the app list
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2);
        req.setDurationMinutes(2);

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);
        ProblemAssertUtil.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_NO_OFFICIAL.getCode(), resp);
    }

    @Test
    public void givenInvalidRequestNotPaid_whenUpdateForClose_then400() throws Exception {
        String[] createdLocation = createAppListUsingRestApi();

        // create an entry
        EntryGetDetailDto entryGetSummaryDto =
                createEntry(
                        UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createdLocation[0])),
                        (dto) -> {
                            FeeStatus feeStatus = new FeeStatus();
                            feeStatus.setStatusDate(LocalDate.now());

                            // not paid so should fail
                            feeStatus.setPaymentStatus(PaymentStatus.UNDERTAKEN);
                            dto.setFeeStatuses(List.of(feeStatus));
                        });

        // create the result for the entry
        createResultSuccess(entryGetSummaryDto.getListId(), entryGetSummaryDto.getId());

        // close the app list
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2);
        req.setDurationMinutes(2);

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);
        ProblemAssertUtil.assertEquals(
                ApplicationListError.INVALID_FOR_CLOSE_NOT_PAID.getCode(), resp);
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
                        .courtLocationCode("UN")
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
                        .cjaCode("UN")
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

    @Test
    public void givenEntryUpdate_whenOpeningClosedList_then400() throws Exception {
        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("update-open-closed-list"));

        // update list to closed
        var updateReq =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Updated description")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(1)
                        .durationMinutes(0);

        Response updateResp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId), token, updateReq);
        updateResp.then().statusCode(HttpStatus.OK.value());

        // attempt to update back to open
        var reopenReq =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Updated description")
                        .status(ApplicationListStatus.OPEN)
                        .durationHours(1)
                        .durationMinutes(0);

        Response reopenResp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId), token, reopenReq);
        reopenResp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // Assert failure is due to invalid list status for update
        ProblemDetail problemDetail = reopenResp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.INVALID_LIST_STATUS.getCode().getAppCode(),
                problemDetail.getType().toString());
    }
}
