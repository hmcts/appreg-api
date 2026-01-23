package uk.gov.hmcts.appregister.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

/**
 * A class that allows us to specifically test the general validation of data constraints on the API
 * and the response to those constraints.
 *
 * <p>This class is vital to ensure we do not leak implementation details through API response.
 */
public class DataConstraintControllerTest extends BaseIntegration {

    private static final String CODE_WEB_CONTEXT = "application-codes";
    private static final String APP_LIST_WEB_CONTEXT = "application-lists";
    private static final String VALID_COURT_CODE = "CCC003";
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);

    @Test
    public void testSizeFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CODE_WEB_CONTEXT + "/TOOOOLOOOONG?date=" + LocalDate.now()),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getType().get(), problemDetail.getType());
        Assertions.assertEquals(
                "getApplicationCodeByCodeAndDate.code: size must be between 0 and 10",
                problemDetail.getDetail());
    }

    @Test
    public void testDateFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CODE_WEB_CONTEXT + "/MS9900723?date=12-12-2025"),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Problem with value 12-12-2025 for parameter date", problemDetail.getDetail());
    }

    @Test
    public void testTimeFailure() throws Exception {
        LocalTime midNight = LocalTime.parse("00:00");
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(midNight)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String requestString = mapper.writeValueAsString(createListReq);

        // replace to use an invalid form of the time
        String invalidTimeRequest = requestString.replace("[0,0]", "\"24:00:23\"");

        // test the functionality
        Response createListResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(APP_LIST_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        invalidTimeRequest);

        // assert the response
        createListResp.then().statusCode(400);
        ProblemDetail problemDetail = createListResp.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Text '24:00:23' could not be parsed, unparsed text found at index 5",
                problemDetail.getDetail());
    }

    @Test
    public void testHourValueMaximumFailure() throws Exception {
        LocalTime midNight = LocalTime.parse("00:00");
        int minutesExceedingMax = 61;
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(midNight)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(minutesExceedingMax);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response createListResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(APP_LIST_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        createListReq);

        // assert the response
        createListResp.then().statusCode(400);
        ProblemDetail problemDetail = createListResp.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Validation failed for fields:durationMinutes=61", problemDetail.getDetail());
    }

    @Test
    public void testStateEnumFailure() throws Exception {
        LocalTime timeofEntry = LocalTime.parse("01:00");
        var createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(timeofEntry)
                        .description("description")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(2)
                        .status(ApplicationListStatus.OPEN);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String requestString = mapper.writeValueAsString(createListReq);

        // replace to use an invalid form of the enum entry
        String invalidStatus =
                requestString.replace(ApplicationListStatus.OPEN.getValue(), "INVALID_STATUS");

        // add a successful time
        invalidStatus = invalidStatus.replace("[1,0]", "\"01:00\"");

        // test the functionality
        Response createListResp =
                restAssuredClient.executePostRequest(
                        getLocalUrl(APP_LIST_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        invalidStatus);

        // assert the response
        createListResp.then().statusCode(400);
        ProblemDetail problemDetail = createListResp.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Not Readable Error. Cant read value from field:status", problemDetail.getDetail());
    }

    @Test
    public void testMismatchFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CODE_WEB_CONTEXT + "/CODE1?date=NOTADATE"),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Problem with value NOTADATE for parameter date", problemDetail.getDetail());
    }

    @Test
    public void testParameterFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CODE_WEB_CONTEXT + "/TOOOOLOOOONG"),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.PARAMETER_REQUIRED.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Required request parameter 'date' is missing", problemDetail.getDetail());
    }
}
