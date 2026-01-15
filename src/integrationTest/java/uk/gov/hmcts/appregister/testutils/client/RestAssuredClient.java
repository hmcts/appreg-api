package uk.gov.hmcts.appregister.testutils.client;

import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.apache.http.HttpHeaders;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeDeserializer;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeSerializer;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

@Component
public class RestAssuredClient {

    @Value("${spring.data.web.pageable.page-parameter}")
    private String pageNumberQueryName;

    @Value("${spring.data.web.pageable.size-parameter}")
    private String pageSizeQueryName;

    @Value("${spring.data.web.sort.sort-parameter}")
    private String sortQueryName;

    // Initialize RestAssured configuration
    {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule timeModule = new JavaTimeModule();

        // Setup the serializer and deserializer for LocalTime with format "HH:mm"
        timeModule.addDeserializer(LocalTime.class, new StrictLocalTimeDeserializer());
        timeModule.addSerializer(LocalTime.class, new StrictLocalTimeSerializer());
        objectMapper.registerModule(timeModule);
        objectMapper.registerModule(new JsonNullableModule());
        RestAssured.config =
                RestAssuredConfig.config()
                        .objectMapperConfig(
                                ObjectMapperConfig.objectMapperConfig()
                                        .jackson2ObjectMapperFactory(
                                                (cls, charset) -> objectMapper));
    }

    /**
     * gets a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executeGetRequest(URL url, TokenAndJwksKey token) throws URISyntaxException {
        return given().header("Authorization", "Bearer " + token.getToken()).get(url).andReturn();
    }

    /**
     * gets a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @param requestSpecificationConsumer The consumer to customise the request specification
     * @return The specification of the response
     */
    public Response executeGetRequest(
            URL url,
            TokenAndJwksKey token,
            UnaryOperator<RequestSpecification> requestSpecificationConsumer)
            throws URISyntaxException {
        return requestSpecificationConsumer
                .apply(given().header("Authorization", "Bearer " + token.getToken()))
                .get(url)
                .andReturn();
    }

    /**
     * gets a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executeGetRequestWithPaging(
            Optional<Integer> pageSize,
            Optional<Integer> pageNumber,
            List<String> pageSort,
            URL url,
            TokenAndJwksKey token,
            PageMetaData pageMetaData) {
        return executeGetRequestWithPaging(
                pageSize, pageNumber, pageSort, url, token, rs -> rs, pageMetaData);
    }

    /**
     * gets a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executeGetRequestWithPaging(
            Optional<Integer> pageSize,
            Optional<Integer> pageNumber,
            List<String> pageSort,
            URL url,
            TokenAndJwksKey token) {
        return executeGetRequestWithPaging(
                pageSize, pageNumber, pageSort, url, token, rs -> rs, new OpenApiPageMetaData());
    }

    /**
     * gets a request builder that can be used to make requests against the application.
     *
     * @param pageSize The page size of the reuest
     * @param pageNumber The page number of the request
     * @param pageSort The page sort number of the request
     * @param url The url context
     * @param token The bearer token
     * @param requestSpecificationConsumer A request specification that will be called before
     *     sending the request. Allows operation specific payload customisation i.e. request
     *     parameters to be added etc
     * @param pageMetaData The meta data for the paging request
     * @return The specification of the response
     */
    public Response executeGetRequestWithPaging(
            Optional<Integer> pageSize,
            Optional<Integer> pageNumber,
            List<String> pageSort,
            URL url,
            TokenAndJwksKey token,
            UnaryOperator<RequestSpecification> requestSpecificationConsumer,
            PageMetaData pageMetaData) {
        return requestSpecificationConsumer
                .apply(
                        applyPageDetails(
                                given().header("Authorization", "Bearer " + token.getToken()),
                                pageNumber,
                                pageSize,
                                pageSort,
                                pageMetaData))
                .get(url)
                .andReturn();
    }

    /**
     * Setup the page details on the request.
     *
     * @param requestSpecification The request specification
     * @param pageNumber The page number
     * @param pageSize The page size
     * @param sortField The sort field
     */
    private RequestSpecification applyPageDetails(
            RequestSpecification requestSpecification,
            Optional<Integer> pageNumber,
            Optional<Integer> pageSize,
            List<String> sortField,
            PageMetaData pageMetaData) {
        if (pageNumber.isPresent()) {
            requestSpecification =
                    requestSpecification.queryParam(
                            pageMetaData == null
                                    ? pageNumberQueryName
                                    : pageMetaData.getPageNumberQueryName(),
                            pageNumber.get());
        }

        if (pageSize.isPresent()) {
            requestSpecification =
                    requestSpecification.queryParam(
                            pageMetaData == null
                                    ? pageSizeQueryName
                                    : pageMetaData.getPageSizeQueryName(),
                            pageSize.get());
        }

        if (!sortField.isEmpty()) {
            for (String sort : sortField) {
                requestSpecification =
                        requestSpecification.queryParam(
                                pageMetaData == null ? sortQueryName : pageMetaData.getSortName(),
                                sort);
            }
        }
        return requestSpecification;
    }

    /**
     * posts a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executePostRequest(URL url, TokenAndJwksKey token, Object object) {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/vnd.hmcts.appreg.v1+json")
                .post(url)
                .andReturn();
    }

    /**
     * posts a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executePostRequest(URL url, TokenAndJwksKey token, String object) {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/vnd.hmcts.appreg.v1+json")
                .post(url)
                .andReturn();
    }

    /**
     * deletes a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executeDeleteRequest(URL url, TokenAndJwksKey token) {
        return given().header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/vnd.hmcts.appreg.v1+json")
                .delete(url)
                .andReturn();
    }

    /**
     * deletes a request builder that can be used to make requests against the application with an
     * If-Match header.
     *
     * @param url The url context
     * @param token The bearer token
     * @param ifMatch The If-Match (ETag) header value
     * @return The specification of the response
     */
    public Response executeDeleteRequest(URL url, TokenAndJwksKey token, String ifMatch) {
        return given().header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/vnd.hmcts.appreg.v1+json")
                .header("If-Match", ifMatch)
                .delete(url)
                .andReturn();
    }

    /**
     * puts a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executePutRequest(URL url, TokenAndJwksKey token, Object object) {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/vnd.hmcts.appreg.v1+json")
                .put(url)
                .andReturn();
    }

    /**
     * puts a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @param etag The etag to use in the request
     * @return The specification of the response
     */
    public Response executePutRequest(URL url, TokenAndJwksKey token, Object object, String etag) {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/vnd.hmcts.appreg.v1+json")
                .header(HttpHeaders.IF_MATCH, etag)
                .put(url)
                .andReturn();
    }
}
