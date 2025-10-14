package uk.gov.hmcts.appregister.applicationlist.validator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;

/**
 * Validator component for {@link ApplicationListCreateDto} location fields.
 *
 * <p>Enforces the business rule that exactly one valid location option is provided: either a court
 * location code, or a combination of criminal justice area code and other location description.
 */
@Component
public class ApplicationListLocationValidator implements Validator<ApplicationListCreateDto> {

    /**
     * Validates the location fields of the given {@link ApplicationListCreateDto}.
     *
     * <p>A DTO is considered valid if and only if one of the following holds (exclusive OR):
     *
     * <ul>
     *   <li>{@code courtLocationCode} is non-null and non-blank
     *   <li>both {@code cjaCode} and {@code otherLocationDescription} are non-null and non-blank
     * </ul>
     *
     * <p>Supplying neither option, or supplying both options at the same time, is invalid.
     *
     * @param dto the DTO to validate (must not be {@code null})
     * @throws AppRegistryException if the location combination does not satisfy the XOR rule
     */
    public void validate(ApplicationListCreateDto dto) {
        String court = dto.getCourtLocationCode();
        String cja = dto.getCjaCode();
        String other = dto.getOtherLocationDescription();

        boolean hasCourt = StringUtils.hasText(court);
        boolean hasCja = StringUtils.hasText(cja);
        boolean hasOther = StringUtils.hasText(other);

        // XOR rule: either courtLocationCode OR (cjaCode AND otherLocationDescription)
        boolean valid = hasCourt ^ (hasCja && hasOther);

        if (!valid) {
            throw new AppRegistryException(
                    ApplicationListError.INVALID_LOCATION_COMBINATION,
                    "Provide either 'courtLocation', or both a 'cja code' and a 'otherLocationDescription'.");
        }
    }
}
