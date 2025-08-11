package uk.gov.hmcts.appregister.applicationlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListDto;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;

@RestController
@RequestMapping("/application-lists")
@RequiredArgsConstructor
public class ApplicationListController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationListController.class);
    private final ApplicationListService listService;

    @Operation(
            summary = "Get all application lists for the authenticated user",
            operationId = "getAllApplicationLists")
    @ApiResponse(responseCode = "200", description = "Application lists successfully retrieved")
    @GetMapping
    public ResponseEntity<List<ApplicationListDto>> getAll(@AuthenticationPrincipal Jwt jwt) {
        log.info("Getting all application lists for user: {}", jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        return ResponseEntity.ok(listService.getAllForUser(userId));
    }

    @Operation(
            summary = "Get a single application list by ID for the authenticated user",
            operationId = "getApplicationListById")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application list found"),
        @ApiResponse(responseCode = "404", description = "Application list not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationListDto> getById(
            @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Getting application list with id: {}, for user: {}",
                id,
                jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        return ResponseEntity.ok(listService.getByIdForUser(id, userId));
    }

    @Operation(
            summary = "Create a new application list for the authenticated user",
            operationId = "createApplicationList")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Application list created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<ApplicationListDto> create(
            @RequestBody ApplicationListWriteDto listDto, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("oid");
        log.info("Creating new application list for user: {}", jwt.getClaimAsString("sub"));
        ApplicationListDto created = listService.create(listDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Update an existing application list for the authenticated user",
            operationId = "updateApplicationList")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application list updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Application list not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationListDto> update(
            @PathVariable Long id,
            @RequestBody ApplicationListWriteDto listDto,
            @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Updating application list with id: {}, for user: {}",
                id,
                jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        ApplicationListDto updated = listService.update(id, listDto, userId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete an application list by ID", operationId = "deleteApplicationList")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Application list deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Application list not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info(
                "Deleting application list with id: {}, for user: {}",
                id,
                jwt.getClaimAsString("sub"));
        String userId = jwt.getClaimAsString("oid");
        listService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
