package uk.gov.hmcts.appregister.csds.ingress;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsIngestError;

@Getter
@RequiredArgsConstructor
public enum CsdsIngestProcessorName {
    APPLICATION_CODES("application_codes"),
    RESOLUTION_CODES("resolution_codes"),
    FEE("fee"),
    NATIONAL_COURT_HOUSES("national_court_houses"),
    STANDARD_APPLICANTS("standard_applicants");

    private final String externalName;

    public static CsdsIngestProcessorName fromExternalName(String externalName) {
        if (externalName == null || externalName.isBlank()) {
            throw new AppRegistryException(
                    CsdsIngestError.INVALID_PROCESSOR, "A CSDS processor name must be provided");
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.externalName.equals(externalName))
                .findFirst()
                .orElseThrow(
                        () ->
                                new AppRegistryException(
                                        CsdsIngestError.INVALID_PROCESSOR,
                                        "Unknown CSDS processor: " + externalName));
    }
}
