package uk.gov.hmcts.appregister.applicationlist.validator;

import java.util.function.Function;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;

/**
 * Validator component for {@link uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto}
 * location fields.
 *
 * <p>Enforces the business rule that exactly one valid location option is provided: either a court
 * location code, or a combination of criminal justice area code and other location description.
 */
@Component
public class ApplicationListGetValidator
        extends AbstractApplicationListLocationValidator<
                ApplicationListGetFilterDto, ListLocationValidationSuccess> {

    public ApplicationListGetValidator(
            ApplicationListRepository applicationListRepository,
            NationalCourtHouseRepository courtHouseRepository,
            CriminalJusticeAreaRepository criminalJusticeAreaRepository) {
        super(applicationListRepository, courtHouseRepository, criminalJusticeAreaRepository);
    }

    @Override
    Function<ApplicationListGetFilterDto, String> getCourtLocation() {
        return ApplicationListGetFilterDto::getCourtLocationCode;
    }

    @Override
    Function<ApplicationListGetFilterDto, String> getCjaCode() {
        return ApplicationListGetFilterDto::getCjaCode;
    }

    @Override
    Function<ApplicationListGetFilterDto, String> getLocationDescription() {
        return ApplicationListGetFilterDto::getOtherLocationDescription;
    }

    @Override
    ListLocationValidationSuccess getResult() {
        return new ListLocationValidationSuccess();
    }
}
