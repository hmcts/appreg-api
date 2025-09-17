package uk.gov.hmcts.appregister.applicationlistentryresolutions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionDto;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionWriteDto;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.service.ApplicationListEntryResolutionService;

/** REST controller for managing application results. */
@RestController
@RequestMapping("/application-lists/{listId}/applications")
@RequiredArgsConstructor
public class ApplicationListEntryResolutionController {

    private final ApplicationListEntryResolutionService resultService;
    private static final Logger log =
            LoggerFactory.getLogger(ApplicationListEntryResolutionController.class);

    @Operation(
            summary = "Get the result for a specific application",
            operationId = "getApplicationResult")
    @ApiResponse(responseCode = "200", description = "Result retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Result not found or not accessible")
    @GetMapping("/{applicationId}/results")
    public ResponseEntity<ApplicationListEntryResolutionDto> getResult(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Getting result for application: {}, owned by this user: {}",
                applicationId,
                jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(resultService.getResultForApplication(listId, applicationId));
    }

    @Operation(
            summary = "Create a result for a specific application",
            operationId = "createApplicationResult")
    @ApiResponse(responseCode = "201", description = "Result created successfully")
    @ApiResponse(responseCode = "404", description = "Application or result code not found")
    @PostMapping("/{applicationId}/results")
    public ResponseEntity<ApplicationListEntryResolutionDto> createResult(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @RequestBody ApplicationListEntryResolutionWriteDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Creating result for application: {}, owned by this user: {}",
                applicationId,
                jwt.getClaimAsString("sub"));
        ApplicationListEntryResolutionDto created =
                resultService.create(listId, applicationId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Update an existing application result",
            operationId = "updateApplicationResult")
    @ApiResponse(responseCode = "200", description = "Result updated successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Result or result code not found or not accessible")
    @PutMapping("/{applicationId}/results/{resultId}")
    public ResponseEntity<ApplicationListEntryResolutionDto> updateResult(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @PathVariable Long resultId,
            @RequestBody ApplicationListEntryResolutionWriteDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Updating result for application: {}, owned by this user: {}",
                applicationId,
                jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(resultService.update(listId, applicationId, resultId, dto));
    }

    @Operation(
            summary = "Delete a result from an application",
            operationId = "deleteApplicationResult")
    @ApiResponse(responseCode = "204", description = "Result deleted successfully")
    @ApiResponse(responseCode = "404", description = "Result not found or not accessible")
    @DeleteMapping("/{applicationId}/results/{resultId}")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long listId,
            @PathVariable Long applicationId,
            @PathVariable Long resultId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Deleting result for application: {}, owned by this user: {}",
                applicationId,
                jwt.getClaimAsString("sub"));
        resultService.delete(listId, applicationId, resultId);
        return ResponseEntity.noContent().build();
    }
}
