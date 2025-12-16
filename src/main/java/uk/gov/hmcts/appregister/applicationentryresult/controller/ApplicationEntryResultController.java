package uk.gov.hmcts.appregister.applicationentryresult.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.service.ApplicationEntryResultService;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ApplicationListEntryResultsApi;

/**
 * REST controller for managing Application List Entry Results.
 */
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryResultController implements ApplicationListEntryResultsApi {

    private final ApplicationEntryResultService service;

    /**
     * Deletes an Application List Entry Result.
     *
     * <p>This endpoint deletes the provided result id and returns a 204 response
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param listId Public identifier of the Application List. (required)
     * @param entryId Public identifier of the Application List Entry. (required)
     * @param resultId Public identifier of the Application List Entry Result. (required)
     * @return {@link ResponseEntity} The 204 response
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> deleteApplicationListEntryResult(
            UUID listId, UUID entryId, UUID resultId) {
        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        service.delete(args);
        log.info("Deleted Application List Entry Result with id: {}", resultId);
        return ResponseEntity.noContent().build();
    }
}
