package uk.gov.hmcts.appregister.courtlocation.controller;

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
import uk.gov.hmcts.appregister.courtlocation.dto.CourtHouseDto;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;

@RestController
@RequestMapping("/courthouses")
@RequiredArgsConstructor
public class CourtHouseController {
    private final CourtLocationService service;

    @Operation(summary = "Get all courthouses", operationId = "getAllCourthouses")
    @ApiResponse(responseCode = "200", description = "List of courthouses retrieved successfully")
    @GetMapping
    public ResponseEntity<List<CourtHouseDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Get a specific courthouse by ID", operationId = "getCourtHouseById")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Courthouse found"),
        @ApiResponse(responseCode = "404", description = "Courthouse not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourtHouseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
