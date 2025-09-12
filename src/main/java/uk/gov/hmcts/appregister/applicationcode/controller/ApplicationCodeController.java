package uk.gov.hmcts.appregister.applicationcode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.service.ApplicationCodeService;
import uk.gov.hmcts.appregister.applicationcode.validator.ApplicationCodeSortValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.security.annotation.AdminRestricted;

/** REST controller for managing application codes. */
@RestController
@RequestMapping("/application-codes")
@RequiredArgsConstructor
public class ApplicationCodeController {
    private final ApplicationCodeService service;

    private final ApplicationCodeSortValidator sortValidator;

    @Operation(summary = "Get all application codes", operationId = "getAllApplicationCodes")
    @ApiResponse(
            responseCode = "200",
            description = "List of application codes retrieved successfully")
    @GetMapping
    @AdminRestricted
    public ResponseEntity<Page<ApplicationCodeDto>> getAll(
            @RequestParam(required = false) String appCode,
            @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false)
                    String appTitle,
            @RequestParam(required = false) OffsetDateTime lodgementDate,
            @org.springframework.data.web.PageableDefault(
                            sort = ApplicationCode_.CODE,
                            direction = org.springframework.data.domain.Sort.Direction.ASC)
                    Pageable pageable) {

        // validate the sort parameters
        pageable.getSort().get().forEach(o -> sortValidator.validate(o.getProperty()));

        return ResponseEntity.ok()
                .body(service.findAll(appCode, appTitle, lodgementDate, pageable));
    }

    @Operation(summary = "Get a single application code by its code")
    @ApiResponse(responseCode = "200", description = "Application code found")
    @ApiResponse(responseCode = "404", description = "Application code not found")
    @GetMapping("/{code}")
    @AdminRestricted
    public ResponseEntity<ApplicationCodeDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.findByCode(code));
    }
}
