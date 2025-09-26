package uk.gov.hmcts.appregister.applicationlist.validator;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

@Component
public class ApplicationListCreateRequestValidator implements Validator<ApplicationListCreateDto> {

    @Override
    public void validate(ApplicationListCreateDto dto) {

        var courtLocation = dto.getCourtLocation();
        var cja = dto.getCriminalJusticeArea();
        boolean valid = hasCourtLocationOrCja(dto, courtLocation, cja);

        if (!valid) {
            throw new AppRegistryException(
                    ApplicationListError.INVALID_LOCATION_COMBINATION,
                    "Provide either 'courtLocation', or both 'criminalJusticeArea' and a non-blank 'otherLocationDescription'.");
        }
    }

    private static boolean hasCourtLocationOrCja(
            ApplicationListCreateDto dto,
            JsonNullable<CourtLocationGetDetailDto> courtLocation,
            JsonNullable<CriminalJusticeAreaGetDto> cja) {
        var otherLocation = dto.getOtherLocationDescription();

        boolean hasCourtLocation = courtLocation != null && courtLocation.isPresent();
        boolean hasCja = cja != null && cja.isPresent();
        boolean hasOtherLocation =
                otherLocation != null
                        && otherLocation.isPresent()
                        && !otherLocation.get().isBlank();

        // Core rule: either courtLocation OR (cja AND otherLocation)
        boolean valid = hasCourtLocation || (hasCja && hasOtherLocation);
        return valid;
    }
}
