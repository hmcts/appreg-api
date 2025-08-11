package uk.gov.hmcts.appregister.resultcode.controller;

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
import uk.gov.hmcts.appregister.resultcode.dto.ResultCodeDto;
import uk.gov.hmcts.appregister.resultcode.service.ResultCodeService;

@RestController
@RequestMapping("/result-codes")
@RequiredArgsConstructor
public class ResultCodeController {
    private final ResultCodeService service;

    @Operation(summary = "Get all result codes", operationId = "getAllResultCodes")
    @ApiResponse(responseCode = "200", description = "List of result codes retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ResultCodeDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Get a result code by code", operationId = "getResultCodeByCode")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Result code found"),
        @ApiResponse(responseCode = "404", description = "Result code not found")
    })
    @GetMapping("/{code}")
    public ResponseEntity<ResultCodeDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.findByCode(code));
    }
}
