package uk.gov.hmcts.appregister.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.dto.read.ApplicationCodeDto;
import uk.gov.hmcts.appregister.service.api.ApplicationCodeService;

@RestController
@RequestMapping("/application-codes")
@RequiredArgsConstructor
public class ApplicationCodeController {
    private final ApplicationCodeService service;

    @Operation(summary = "Get all application codes", operationId = "getAllApplicationCodes")
    @ApiResponse(
            responseCode = "200",
            description = "List of application codes retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ApplicationCodeDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Get a single application code by its code")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application code found"),
        @ApiResponse(responseCode = "404", description = "Application code not found")
    })
    @GetMapping("/{code}")
    public ResponseEntity<ApplicationCodeDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.findByCode(code));
    }
}
