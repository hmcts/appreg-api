package uk.gov.hmcts.appregister.applicationlist.validator;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;

/**
 * A successful output come of {@link
 * uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator}.
 */
@Getter
@RequiredArgsConstructor
@Setter
public class ListLocationValidationSuccess {
    private NationalCourtHouse nationalCourtHouse;
    private CriminalJusticeArea criminalJusticeArea;

    public boolean hasCourt() {
        return Optional.ofNullable(nationalCourtHouse).isPresent();
    }
}
