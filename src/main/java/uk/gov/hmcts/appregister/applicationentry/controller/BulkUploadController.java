package uk.gov.hmcts.appregister.applicationentry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.dto.BulkUploadResponseDto;
import uk.gov.hmcts.appregister.applicationentry.service.BulkUploadService;

@RestController
@RequestMapping("/application-lists")
@RequiredArgsConstructor
public class BulkUploadController {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadController.class);
    private final BulkUploadService bulkUploadService;

    @Operation(
            summary = "Bulk upload application entries for a specific list",
            operationId = "bulkUploadEntries")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV processed successfully"),
        @ApiResponse(responseCode = "400", description = "Malformed file or invalid data"),
        @ApiResponse(responseCode = "404", description = "Application list not found")
    })
    @PostMapping("/{listId}/applications/bulk-upload")
    public ResponseEntity<BulkUploadResponseDto> uploadEntries(
            @PathVariable Long listId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Bulk uploading entries to listId: {}", listId);
        String userId = jwt.getClaimAsString("oid");
        BulkUploadResponseDto response = bulkUploadService.uploadCsv(listId, file, userId);
        return ResponseEntity.ok(response);
    }
}
