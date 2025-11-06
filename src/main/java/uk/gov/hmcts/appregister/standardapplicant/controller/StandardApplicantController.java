package uk.gov.hmcts.appregister.standardapplicant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.CriminalJusticeAreasApi;
import uk.gov.hmcts.appregister.generated.api.StandardApplicantsApi;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.standardapplicant.service.StandardApplicantService;

import static org.springframework.http.HttpStatus.OK;

/**
 * Controller for managing standard applicants.
 */
@RestController
@RequiredArgsConstructor
public class StandardApplicantController implements StandardApplicantsApi {
    private final StandardApplicantService service;

    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    @Operation(
            summary = "Get all standard applicants for the authenticated user",
            operationId = "getAllStandardApplicants")
    @ApiResponse(responseCode = "200", description = "Standard applicants retrieved successfully")
    @GetMapping
    @Deprecated
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/standard-applicants"
    )
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<List<StandardApplicantDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantGetDetailDto> getStandardApplicantByCodeAndDate(String code, LocalDate date) {

        StandardApplicantGetDetailDto standardApplicantGetDetailDto = service.findByCode(code, date);

        return ResponseEntity.status(OK)
                        .varyBy("Accept")
                        .contentType(VND_JSON_V1)
                        .body(standardApplicantGetDetailDto);
    }
}
