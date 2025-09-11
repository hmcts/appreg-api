package uk.gov.hmcts.appregister.applicationentry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationListEntryDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationListEntryService;

@RestController
@RequestMapping("/application-lists/{listId}/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);
    private final ApplicationListEntryService appService;

    @Operation(
            summary = "Get all applications for a specific application list",
            operationId = "getAllApplications")
    @ApiResponse(responseCode = "200", description = "Applications retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ApplicationListEntryDto>> getAll(
            @PathVariable Long listId, @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Getting all applications linked to listId: {} and user: {}",
                listId,
                jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(appService.getAllByListId(listId));
    }

    @Operation(
            summary = "Create a new application within an application list",
            operationId = "createApplication")
    @ApiResponse(responseCode = "201", description = "Application created successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid standard applicant or application code")
    @ApiResponse(responseCode = "404", description = "Application list not found")
    @PostMapping
    public ResponseEntity<ApplicationListEntryDto> create(
            @PathVariable Long listId,
            @RequestBody ApplicationWriteDto application,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Creating new application for user: {}", jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        ApplicationListEntryDto created = appService.create(listId, application);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Get a specific application by ID within a list",
            operationId = "getApplicationById")
    @ApiResponse(responseCode = "200", description = "Application retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Application not found or not accessible")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationListEntryDto> getOne(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Getting application with id: {}, for listId: {} and user: {}",
                applicationId,
                listId,
                jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        return ResponseEntity.ok(appService.getByIdForUser(listId, applicationId));
    }

    @Operation(summary = "Update an existing application", operationId = "updateApplication")
    @ApiResponse(responseCode = "200", description = "Application updated successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid standard applicant or application code")
    @ApiResponse(responseCode = "404", description = "Application not found or not accessible")
    @PutMapping("/{applicationId}")
    public ResponseEntity<ApplicationListEntryDto> update(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @RequestBody ApplicationWriteDto updatedApplication,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Updating application with id: {}, for listId: {} and user: {}",
                listId,
                applicationId,
                jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        ApplicationListEntryDto updated =
                appService.update(listId, applicationId, updatedApplication);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete an application by ID within a list",
            operationId = "deleteApplication")
    @ApiResponse(responseCode = "204", description = "Application deleted successfully")
    @ApiResponse(responseCode = "404", description = "Application not found or not accessible")
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Deleting application list with id: {}, for listId: {} and user: {}",
                listId,
                applicationId,
                jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        appService.delete(listId, applicationId);
        return ResponseEntity.noContent().build();
    }
}
