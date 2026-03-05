package uk.gov.hmcts.appregister.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

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

    @Autowired protected TransactionalUnitOfWork unitOfWork;

    @Autowired protected ApplicationListRepository applicationListRepository;

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
    public void testSizeBodyFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ApplicationListCreateDto createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(LocalTime.parse("01:00"))
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode("TOOOOLOOOONG")
                        .durationHours(1)
                        .durationMinutes(2);

        // test the functionality
        Response responseSpec =
                restAssuredClient.executePostRequest(
                        getLocalUrl(APP_LIST_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        createListReq);

        // assert the response
        responseSpec.then().statusCode(400);
        String expectedJson =
                """
                {"type":"COMMON-11","title":"Method Error",
                "status":400,"detail":"Validation failed for fields:",
                "instance":"/application-lists",
                "errors":{"courtLocationCode":
                "size must be between 0 and 10",
                "description":"must not be null"}}
            """;

        JSONAssert.assertEquals(expectedJson, responseSpec.asString(), true);
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
    public void testTimeOnQueryFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response createListResp =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(APP_LIST_WEB_CONTEXT + "?time=24:00:23"),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        createListResp.then().statusCode(400);
        createListResp.asString();

        String expectedJson =
                """
                {"type":"COMMON-11","title":"Method Error","status":400,"detail":
                "Validation failed for fields:","instance":"/application-lists","errors":
                {"time":"Please ensure that any times are in the format HH:mm and dates are in the format yyyy-MM-dd"}}
            """;

        JSONAssert.assertEquals(expectedJson, createListResp.asString(), true);
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

        String expectedJson =
                """
               {"type":"COMMON-11","title":"Method Error",
               "status":400,"detail":"Validation failed for fields:",
               "instance":"/application-lists",
               "errors":{"durationMinutes":"must be less than or equal to 59"}}
            """;

        JSONAssert.assertEquals(expectedJson, createListResp.asString(), true);
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
                "Type conversion problem. Something in the payload is not correct",
                problemDetail.getDetail());
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

    @Test
    public void testNumberAsString() throws Exception {
        ApplicationListCreateDto createListReq =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(LocalTime.parse("01:00"))
                        .status(ApplicationListStatus.OPEN)
                        .durationHours(-10)
                        .durationMinutes(2);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String requestString = mapper.writeValueAsString(createListReq);
        requestString = requestString.replace("-10", "\"invalid\"");
        requestString = requestString.replace("[1,0]", "\"01:00\"");

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executePostRequest(
                        getLocalUrl(APP_LIST_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        requestString);

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                problemDetail.getType());
        Assertions.assertEquals(
                "Problem setting value for durationHours please"
                        + " check the correct type is used",
                problemDetail.getDetail());
    }

    @Test
    public void testRegexFailureApplicant() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.getApplicant().getPerson().getName().setFirstForename("TEST\tSURNAME");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("TEST\rSURNAME"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("TEST\nThird Forename"));
        entryCreateDto.getApplicant().getPerson().getName().setSurname("TEST SURNAME \0");

        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine1("TEST ADDRESS LINE 1");
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("TEST ADDRESS LINE 2 \u0000"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of("Testshire \u0001"));
        entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA-A 1AA");

        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("-4444A444444"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("TESTPHONE"));
        entryCreateDto
                .getApplicant()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("testemail.com"));

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                APP_LIST_WEB_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorNode = mapper.readTree(responseSpecCreate.getBody().print());
        Assertions.assertNotNull(errorNode);

        // Name validation assertions
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.name.surname").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.name.firstForename").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.name.secondForename").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.name.thirdForename").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");

        // Address validation assertions
        Assertions.assertEquals(
                errorNode
                        .get("errors")
                        .get("applicant.person.contactDetails.addressLine2")
                        .asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode
                        .get("errors")
                        .get("applicant.person.contactDetails.addressLine5")
                        .asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.contactDetails.postcode").asText(),
                "must match \"^([A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}|GIR ?0A{2})$\"");

        // Phone number validation assertions
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.contactDetails.mobile").asText(),
                "must match \"^(?:\\+\\d{1,4}\\s*)?[0-9 \\-]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.contactDetails.phone").asText(),
                "must match \"[0-9 \\-]*\"");

        // email validation assertions
        Assertions.assertEquals(
                errorNode.get("errors").get("applicant.person.contactDetails.email").asText(),
                "must match \"^((([^<>()\\[\\]\\\\.,;:\\s@\"]+"
                        + "(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@"
                        + "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])"
                        + "|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,})))*$\"");
    }

    @Test
    public void testRegexFailureRespondent() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        entryCreateDto.getRespondent().getPerson().getName().setFirstForename("TEST\tSURNAME");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setSecondForename(JsonNullable.of("TEST\rSURNAME"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getName()
                .setThirdForename(JsonNullable.of("TEST\nThird Forename"));
        entryCreateDto.getRespondent().getPerson().getName().setSurname("TEST SURNAME \0");

        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine1("TEST ADDRESS LINE 1");
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine2(JsonNullable.of("TEST ADDRESS LINE 2 \u0000"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine3(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine4(JsonNullable.of(null));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setAddressLine5(JsonNullable.of("Testshire \u0001"));
        entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA- 1AA");

        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setMobile(JsonNullable.of("-4444A444444"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setPhone(JsonNullable.of("TESTPHONE"));
        entryCreateDto
                .getRespondent()
                .getPerson()
                .getContactDetails()
                .setEmail(JsonNullable.of("testemail.com"));

        Response responseSpecCreate =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                APP_LIST_WEB_CONTEXT
                                        + "/"
                                        + getOpenApplicationListId()
                                        + "/entries"),
                        tokenGenerator.fetchTokenForRole(),
                        entryCreateDto);

        responseSpecCreate.then().statusCode(400);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode errorNode = mapper.readTree(responseSpecCreate.getBody().print());
        Assertions.assertNotNull(errorNode);

        // Name validation assertions
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.name.surname").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.name.firstForename").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.name.secondForename").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.name.thirdForename").asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");

        // Address validation assertions
        Assertions.assertEquals(
                errorNode
                        .get("errors")
                        .get("respondent.person.contactDetails.addressLine2")
                        .asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode
                        .get("errors")
                        .get("respondent.person.contactDetails.addressLine5")
                        .asText(),
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.contactDetails.postcode").asText(),
                "must match \"^([A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}|GIR ?0A{2})$\"");

        // Phone number validation assertions
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.contactDetails.mobile").asText(),
                "must match \"^(?:\\+\\d{1,4}\\s*)?[0-9 \\-]*$\"");
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.contactDetails.phone").asText(),
                "must match \"[0-9 \\-]*\"");

        // email validation assertions
        Assertions.assertEquals(
                errorNode.get("errors").get("respondent.person.contactDetails.email").asText(),
                "must match \"^((([^<>()\\[\\]\\\\.,;:\\s@\"]+"
                        + "(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@"
                        + "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])"
                        + "|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,})))*$\"");
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
}
