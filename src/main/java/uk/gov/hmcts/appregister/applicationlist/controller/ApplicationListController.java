package uk.gov.hmcts.appregister.applicationlist.controller;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListCreateRequestValidator;
import uk.gov.hmcts.appregister.generated.api.ApplicationListsApi;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

@RestController
@Validated
@RequiredArgsConstructor
public class ApplicationListController implements ApplicationListsApi {

    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final ApplicationListService service;
    private final ApplicationListCreateRequestValidator validator;

    @Override
    public ResponseEntity<ApplicationListGetDetailDto> createApplicationList(
            @Valid ApplicationListCreateDto applicationListCreateDto) {

        // Domain-level request validation beyond bean validation:
        validator.validate(applicationListCreateDto);

        // Persist & map to response
        ApplicationListGetDetailDto created = service.create(applicationListCreateDto);

        // Defensive: ensure ID is present for Location header
        UUID id = created.getId();
        if (id == null) {
            throw new IllegalStateException(
                    "Service returned a created Application List without an id.");
        }

        return ResponseEntity.status(CREATED)
                .contentType(VND_JSON_V1)
                .header(HttpHeaders.LOCATION, locationFor(id))
                .body(created);
    }

    private String locationFor(UUID id) {
        // Keep it relative; if you prefer absolute, inject base URL config here.
        return "/application-lists/" + id;
    }
}
