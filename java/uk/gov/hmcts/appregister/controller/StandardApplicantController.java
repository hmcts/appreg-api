package uk.gov.hmcts.appregister.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.dto.read.StandardApplicantDto;
import uk.gov.hmcts.appregister.service.api.StandardApplicantService;

@RestController
@RequestMapping("/standard-applicants")
@RequiredArgsConstructor
public class StandardApplicantController {
    private final StandardApplicantService service;

    @Operation(
        summary = "Get all standard applicants for the authenticated user",
        operationId = "getAllStandardApplicants"
    )
    @ApiResponse(responseCode = "200", description = "Standard applicants retrieved successfully")
    @GetMapping
    public ResponseEntity<List<StandardApplicantDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(
        summary = "Get a standard applicant by ID",
        operationId = "getStandardApplicantById"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Standard applicant found"),
        @ApiResponse(responseCode = "404", description = "Standard applicant not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardApplicantDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
