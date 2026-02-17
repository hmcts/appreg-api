package uk.gov.hmcts.appregister.testutils.util;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class HeaderUtil {
    /**
     * gets the location header value for the given response.
     *
     * @param response The response to get the location header from
     * @return The location header
     * @throws MalformedURLException if the location header is not a valid URL
     */
    public static URL getLocation(Response response) throws MalformedURLException {
        return URI.create(response.getHeader("Location")).toURL();
    }

    /**
     * gets the trailing id from the location header value for the given response.
     *
     * @param locationString The location header value to get the trailing id from
     * @return The trailing id from the location header value
     */
    public static String getTrailingIdFromLocation(String locationString) {
        return locationString.substring(locationString.lastIndexOf("/") + 1);
    }

    /**
     * gets the etag header value for the given response.
     *
     * @param response The response to get the etag header from
     * @return The etag header
     */
    public static String getETag(Response response) {
        return response.getHeader("ETag");
    }
}
