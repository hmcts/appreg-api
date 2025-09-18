package uk.gov.hmcts.appregister.testutils.client;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.net.URISyntaxException;
import java.net.URL;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;

@Component
public class RestAssuredClient {

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
