package uk.gov.hmcts.appregister.applicationlist.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.appregister.applicationlist.service.ActionsService;
import uk.gov.hmcts.appregister.generated.api.ActionsApi;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

import java.util.UUID;

import static org.springframework.http.HttpStatus.OK;

/**
 * REST controller for managing Application List Entries.
 *
 * <p>This controller provides endpoints for task-based operations that perform domain-specific actions
 * across one or more resources, such as bulk-resulting entries, starting asynchronous
 * bulk uploads, and moving entries between lists. It leverages
 * {@link ActionsService} for business logic and ensures request validation and
 * authorization via Spring Security annotations.
 *
 * <p>Responses are served in versioned JSON media type: {@code
 * application/vnd.hmcts.appreg.v1+json}. Annotations:
 *
 * <ul>
 *   <li>{@code @RestController} - Marks this as a REST controller.
 *   <li>{@code @Validated} - Enables validation on method parameters.
 *   <li>{@code @RequiredArgsConstructor} - Generates a constructor for final fields.
 *   <li>{@code @Slf4j} - Provides logging support.
 * </ul>
 */
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class ActionsController implements ActionsApi {

    private static final MediaType VND_JSON_V1 =
        MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final ActionsService service;

    @Override
    public ResponseEntity<Void> moveApplicationListEntries(
        UUID listId, MoveEntriesDto moveEntriesDto) {
        service.move(listId, moveEntriesDto);

        return ResponseEntity.ok().build();
    }
}
