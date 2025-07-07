package uk.gov.hmcts.appregister.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.appregister.dto.read.ApplicationResultDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationResultWriteDto;
import uk.gov.hmcts.appregister.service.api.ApplicationResultService;

@RestController
@RequestMapping("/application-lists/{listId}/applications")
@RequiredArgsConstructor
public class ApplicationResultController {

    private final ApplicationResultService resultService;
    private final static Logger log = LoggerFactory.getLogger(ApplicationResultController.class);

    @Operation(
        summary = "Get the result for a specific application",
        operationId = "getApplicationResult"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Result retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Result not found or not accessible")
    })
    @GetMapping("/{applicationId}/results")
    public ResponseEntity<ApplicationResultDto> getResult(
        @PathVariable Long listId,
        @PathVariable Long applicationId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Getting result for application: {}, owned by this user: {}", applicationId, jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        return ResponseEntity.ok(resultService.getResultForApplication(listId, applicationId, userId));
    }

    @Operation(
        summary = "Create a result for a specific application",
        operationId = "createApplicationResult"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Result created successfully"),
        @ApiResponse(responseCode = "404", description = "Application or result code not found")
    })
    @PostMapping("/{applicationId}/results")
    public ResponseEntity<ApplicationResultDto> createResult(
        @PathVariable Long listId,
        @PathVariable Long applicationId,
        @RequestBody ApplicationResultWriteDto dto,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Creating result for application: {}, owned by this user: {}", applicationId, jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        ApplicationResultDto created = resultService.create(listId, applicationId, dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Update an existing application result",
        operationId = "updateApplicationResult"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Result updated successfully"),
        @ApiResponse(responseCode = "404", description = "Result or result code not found or not accessible")
    })
    @PutMapping("/{applicationId}/results/{resultId}")
    public ResponseEntity<ApplicationResultDto> updateResult(
        @PathVariable Long listId,
        @PathVariable Long applicationId,
        @PathVariable Long resultId,
        @RequestBody ApplicationResultWriteDto dto,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Updating result for application: {}, owned by this user: {}", applicationId, jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        return ResponseEntity.ok(resultService.update(listId, applicationId, resultId, dto, userId));
    }

    @Operation(
        summary = "Delete a result from an application",
        operationId = "deleteApplicationResult"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Result deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Result not found or not accessible")
    })
    @DeleteMapping("/{applicationId}/results/{resultId}")
    public ResponseEntity<Void> deleteResult(
        @PathVariable Long listId,
        @PathVariable Long applicationId,
        @PathVariable Long resultId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Deleting result for application: {}, owned by this user: {}", applicationId, jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        resultService.delete(listId, applicationId, resultId, userId);
        return ResponseEntity.noContent().build();
    }
}
