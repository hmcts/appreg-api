package uk.gov.hmcts.appregister.testutils.util;

import io.restassured.response.Response;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class HeaderUtil {

    /**
     * gets the location header value for the given resource.
     * @return The location header
     * @throws MalformedURLException if the location header is not a valid URL
     */
    public static URL getLocation(Response response) throws MalformedURLException {
        return URI.create(response.getHeader("Location")).toURL();
    }

    /**
     * gets the etag header value for the given resource.
     * @return The etag header
     */
    public static String getETag(Response response) {
        return response.getHeader("ETag");
    }
}
