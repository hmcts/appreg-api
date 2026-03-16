package uk.gov.hmcts.appregister.common.health;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A test for developers to assert unimplemented endpoints. Developers need to remove from the list
 * when endpoints are implemented. This class allows developers the ability to eye ball the health
 * status of the implementation and manually declare this by removing from the list.
 */
public class ImplementedRestfulTest {
    // The unimplemented endpoints. This list should be empty at the point of releasing
    // application register to production.
    private static List<String> UNIMPLEMENTED_ENDPOINTS =
            List.of(
                    "POST /application-lists/{listId}/entries/results",
                    "POST /application-lists/{listId}/entries/bulk-import",
                    "DELETE /application-lists/{listId}/entries/{entryId}",
                    "GET /application-lists/{listId}/entries",
                    "GET /application-lists/{listId}/entries/{entryId}/results",
                    "GET /jobs/{jobId}",
                    "POST /reports/private-prosecutors-index/jobs",
                    "POST /reports/activity-audit/jobs",
                    "POST /reports/list-maintenance/jobs",
                    "POST /reports/search-warrants/jobs",
                    "GET /reports/jobs/{jobId}/download",
                    "POST /reports/duration/jobs",
                    "POST /reports/fees/jobs",
                    "POST /reports/search-warrants/jobs",
                    "GET /admin/jobs/{jobId}",
                    "POST /admin/jobs/{jobType}");

    @Test
    public void testShouldNotBeImplemented() throws Exception {
        Map<String, Object> implemented = new RestImplementedStatusHealthIndicator().status();

        Assertions.assertTrue(implemented.size() > 0);

        // assert against the unimplemented endpoints.
        for (String endpoint : UNIMPLEMENTED_ENDPOINTS) {
            System.out.println(endpoint);
            Assertions.assertEquals(
                    RestImplementedStatusHealthIndicator.NOT_IMPLEMENTED,
                    implemented.get(endpoint),
                    endpoint
                            + " should be unimplemented. "
                            + "Please remove from the list if this endpoint is implemented.");
        }

        // make sure the count of unimplemented is
        // as expected
        Assertions.assertEquals(
                UNIMPLEMENTED_ENDPOINTS.size(),
                countUnimplementedEndpoints(implemented),
                "The number of unimplemented endpoints has changed. "
                        + " Please update the list of unimplemented endpoints accordingly.");
    }

    private int countUnimplementedEndpoints(Map<String, Object> implemented) {
        int count = 0;
        for (String endpoint : implemented.keySet()) {
            if (RestImplementedStatusHealthIndicator.NOT_IMPLEMENTED.equals(
                    implemented.get(endpoint))) {
                count++;
            }
        }
        return count;
    }
}
