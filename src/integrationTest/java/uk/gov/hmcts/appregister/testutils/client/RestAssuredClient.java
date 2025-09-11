package uk.gov.hmcts.appregister.testutils.client;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.testutils.stubs.TokenAndJwksKey;

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
            Optional<String> pageSort,
            URL url,
            TokenAndJwksKey token)
            throws URISyntaxException {
        return executeGetRequestWithPaging(pageSize, pageNumber, pageSort, url, token, rs -> rs);
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
     */
    public Response executeGetRequestWithPaging(
            Optional<Integer> pageSize,
            Optional<Integer> pageNumber,
            Optional<String> pageSort,
            URL url,
            TokenAndJwksKey token,
            Function<RequestSpecification, RequestSpecification> requestSpecificationConsumer) {
        return requestSpecificationConsumer
                .apply(
                        applyPageDetails(
                                given().queryParam(pageNumberQueryName, pageNumber)
                                        .header("Authorization", "Bearer " + token.getToken()),
                                pageNumber,
                                pageSize,
                                pageSort))
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
            Optional<String> sortField) {
        if (pageNumber.isPresent()) {
            requestSpecification =
                    requestSpecification.queryParam(pageNumberQueryName, pageNumber.get());
        }

        if (pageSize.isPresent()) {
            requestSpecification =
                    requestSpecification.queryParam(pageSizeQueryName, pageSize.get());
        }

        if (sortField.isPresent()) {
            requestSpecification = requestSpecification.queryParam(sortQueryName, sortField.get());
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
                .post(url)
                .andReturn();
    }
}
