package uk.gov.hmcts.appregister.applicationentry.validator;

import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry_;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse_;
import uk.gov.hmcts.appregister.common.validator.AbstractSortValidator;

import org.springframework.stereotype.Component;

/**
 * Sort validator for Application List Entry queries.
 *
 * <p>Restricts sorting to a predefined set of allowed properties to prevent invalid or unsafe
 * database access through arbitrary sort fields.
 */
@Component
public class ApplicationListEntrySortValidator extends AbstractSortValidator {

    /**
     * Creates a validator with allowed sort properties for Application List Entries.
     *
     * <p>Currently limited to "sequence number".
     */
    public ApplicationListEntrySortValidator() {
        super(ApplicationListEntry_.SEQUENCE_NUMBER);
    }
}
