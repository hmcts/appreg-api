package uk.gov.hmcts.appregister.testutils.util;

import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.generated.model.Page;

/**
 * Asserts around the new paging open api specification. See {@link
 * uk.gov.hmcts.appregister.generated.model.Page}
 */
public class PagingAssertionUtil {
    public static void assertPageDetails(
            Page response, int pageSize, int pageNumber, int totalPages, int totalElements) {
        Assertions.assertEquals(pageSize, response.getPageSize());
        Assertions.assertEquals(pageNumber, response.getPageNumber());
        Assertions.assertEquals(totalElements, response.getTotalElements());
        Assertions.assertEquals(totalPages, response.getTotalPages());
    }
}
