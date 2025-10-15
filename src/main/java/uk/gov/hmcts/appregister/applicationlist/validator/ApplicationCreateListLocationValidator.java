package uk.gov.hmcts.appregister.applicationlist.validator;

import java.util.function.Function;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;

/**
 * Validator component for {@link ApplicationListCreateDto} location fields.
 *
 * <p>Enforces the business rule that exactly one valid location option is provided: either a court
 * location code, or a combination of criminal justice area code and other location description.
 */
@Component
public class ApplicationCreateListLocationValidator
        extends AbstractApplicationListLocationValidator<
                ApplicationListCreateDto, ListLocationValidationSuccess> {

    public ApplicationCreateListLocationValidator(
            ApplicationListRepository applicationListRepository,
            NationalCourtHouseRepository courtHouseRepository,
            CriminalJusticeAreaRepository criminalJusticeAreaRepository) {
        super(applicationListRepository, courtHouseRepository, criminalJusticeAreaRepository);
    }

    @Override
    Function<ApplicationListCreateDto, String> getCourtLocation() {
        return ApplicationListCreateDto::getCourtLocationCode;
    }

    @Override
    Function<ApplicationListCreateDto, String> getCjaCode() {
        return ApplicationListCreateDto::getCjaCode;
    }

    @Override
    Function<ApplicationListCreateDto, String> getLocationDescription() {
        return ApplicationListCreateDto::getOtherLocationDescription;
    }

    @Override
    ListLocationValidationSuccess getResult() {
        return new ListLocationValidationSuccess();
    }
}
