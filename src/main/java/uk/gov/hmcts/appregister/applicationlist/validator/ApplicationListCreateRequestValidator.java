package uk.gov.hmcts.appregister.applicationlist.validator;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;

@Component
public class ApplicationListCreateRequestValidator implements Validator<ApplicationListCreateDto> {

    @Override
    public void validate(ApplicationListCreateDto dto) {

        boolean hasCourtLocation = dto.getCourtLocation() != null;
        boolean hasCja = dto.getCriminalJusticeArea() != null;
        boolean hasOtherLocation = dto.getOtherLocationDescription() != null
            && !dto.getOtherLocationDescription().isBlank();

        boolean valid = hasCourtLocation || (hasCja && hasOtherLocation);

        if (!valid) {
            throw new AppRegistryException(
                    ApplicationListError.INVALID_LOCATION_COMBINATION,
                    "Provide either 'courtLocation', or both 'criminalJusticeArea' and a non-blank 'otherLocationDescription'.");
        }
    }
}
