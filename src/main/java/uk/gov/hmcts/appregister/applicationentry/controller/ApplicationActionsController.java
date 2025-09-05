package uk.gov.hmcts.appregister.applicationentry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationMoveRequestDto;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationActionsService;

/** Controller for application actions such as moving applications between lists. */
@RestController
@RequestMapping("/application-actions")
@RequiredArgsConstructor
public class ApplicationActionsController {

  private final ApplicationActionsService actionsService;

  @Operation(
      summary = "Move multiple applications to a different application list",
      operationId = "moveApplications")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Applications moved successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User is not allowed to move one or more applications"),
    @ApiResponse(responseCode = "404", description = "Target application list not found")
  })
  @PostMapping("/move")
  public ResponseEntity<Void> moveApplications(
      @RequestBody ApplicationMoveRequestDto request, @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getClaimAsString("oid");
    actionsService.moveApplications(request.applicationIds(), request.targetListId(), userId);
    return ResponseEntity.noContent().build();
  }
}
