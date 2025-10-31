package uk.gov.hmcts.appregister.audit.listener.diff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a difference between two objects for a specific field.
 */
@Getter
@RequiredArgsConstructor
public class Difference {
    /** The fieldName i.e. (database column) containing the difference */
    private final String field;

    private final String oldValue;
    private final String newValue;
}
