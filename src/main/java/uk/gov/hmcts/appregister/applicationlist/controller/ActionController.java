package uk.gov.hmcts.appregister.applicationlist.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationlist.service.ActionService;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ActionsApi;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

/**
 * REST controller for managing Application List Entries.
 *
 * <p>This controller provides endpoints for task-based operations that perform domain-specific
 * actions across one or more resources, such as bulk-resulting entries, starting asynchronous bulk
 * uploads, and moving entries between lists. It leverages {@link ActionService} for business logic
 * and ensures request validation and authorization via Spring Security annotations.
 *
 * <p>Responses are served in versioned JSON media type: {@code
 * application/vnd.hmcts.appreg.v1+json}. Annotations:
 *
 * <ul>
 *   <li>{@code @RestController} - Marks this as a REST controller.
 *   <li>{@code @Validated} - Enables validation on method parameters.
 *   <li>{@code @RequiredArgsConstructor} - Generates a constructor for final fields.
 * </ul>
 */
@RestController
@Validated
@RequiredArgsConstructor
public class ActionController implements ActionsApi {

    private final ActionService service;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> moveApplicationListEntries(
            UUID listId, MoveEntriesDto moveEntriesDto) {
        service.move(listId, moveEntriesDto);

        return ResponseEntity.ok().build();
    }
}
