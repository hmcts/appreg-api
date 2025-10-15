package uk.gov.hmcts.appregister.applicationlist.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

/**
 * A successful output come of {@link uk.gov.hmcts.appregister.applicationlist.validator
 * .ApplicationUpdateListLocationValidator}.
 */
@Getter
@RequiredArgsConstructor
@Setter
public class ListUpdateValidationSuccess extends ListLocationValidationSuccess {
    /** The application list being updated. */
    private ApplicationList applicationList;
}
