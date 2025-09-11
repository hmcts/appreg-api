package uk.gov.hmcts.appregister.testutils.util;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

/** A utility that allows us to help with the parsing and asserting around paging responses. */
public class PagingUtil {
    // The paging json keys. These are bound to the Spring paging API.
    private static final String PAGE_SIZE_JSON_KEY = "pageable.pageSize";
    private static final String PAGE_NUMBER_JSON_KEY = "pageable.pageNumber";
    private static final String TOTAL_ELEMENTS_JSON_KEY = "totalElements";
    private static final String TOTAL_PAGES_JSON_KEY = "totalPages";
    private static final String PAGEABLE_PAYLOAD_CONTENT_JSON_KEY = "content";

    public static void assertPageDetails(
            Response response, int pageSize, int pageNumber, int totalPages, int totalElements) {
        Assertions.assertEquals(Integer.toString(pageSize), getPageSize(response));
        Assertions.assertEquals(Integer.toString(pageNumber), getPageNumber(response));
        Assertions.assertEquals(Integer.toString(totalElements), getTotalElements(response));
        Assertions.assertEquals(Integer.toString(totalPages), getTotalPages(response));
    }

    public static String getPageSize(Response response) {
        return response.jsonPath().getString(PAGE_SIZE_JSON_KEY);
    }

    public static String getPageNumber(Response response) {
        return response.jsonPath().getString(PAGE_NUMBER_JSON_KEY);
    }

    public static String getTotalElements(Response response) {
        return response.jsonPath().getString(TOTAL_ELEMENTS_JSON_KEY);
    }

    public static String getTotalPages(Response response) {
        return response.jsonPath().getString(TOTAL_PAGES_JSON_KEY);
    }

    public static <T> T getResponseContentFromPagingResponse(
            Response response, Class<T> payloadClass) {
        return response.jsonPath().getObject(PAGEABLE_PAYLOAD_CONTENT_JSON_KEY, payloadClass);
    }
}
