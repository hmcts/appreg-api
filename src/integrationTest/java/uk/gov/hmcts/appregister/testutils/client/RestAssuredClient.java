package uk.gov.hmcts.appregister.testutils.client;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

@Component
public class RestAssuredClient {

    @Value("${spring.data.web.pageable.page-parameter}")
    private String pageNumberQueryName;

    @Value("${spring.data.web.pageable.size-parameter}")
    private String pageSizeQueryName;

    @Value("${spring.data.web.sort.sort-parameter}")
    private String sortQueryName;

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
                pageSize, pageNumber, pageSort, url, token, rs -> rs, null);
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
     * @return The specification of the response
     * @deprecated Please use the overload that takes a PageMetaData parameter
     */
    @Deprecated(forRemoval = true)
    public Response executeGetRequestWithPaging(
            Optional<Integer> pageSize,
            Optional<Integer> pageNumber,
            List<String> pageSort,
            URL url,
            TokenAndJwksKey token,
            UnaryOperator<RequestSpecification> requestSpecificationConsumer) {
        return executeGetRequestWithPaging(
                pageSize, pageNumber, pageSort, url, token, requestSpecificationConsumer, null);
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
    public Response executePostRequest(URL url, TokenAndJwksKey token, Object object)
            throws URISyntaxException {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/json")
                .post(url)
                .andReturn();
    }

    /**
     * puts a request builder that can be used to make requests against the application.
     *
     * @param url The url context
     * @param token The bearer token
     * @return The specification of the response
     */
    public Response executePutRequest(URL url, TokenAndJwksKey token, Object object)
            throws URISyntaxException {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/json")
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
    public Response executePutRequest(URL url, TokenAndJwksKey token, Object object, String etag)
            throws URISyntaxException {
        return given().body(object)
                .header("Authorization", "Bearer " + token.getToken())
                .header("Content-Type", "application/json")
                .header(HttpHeaders.IF_MATCH, etag)
                .put(url)
                .andReturn();
    }
}
