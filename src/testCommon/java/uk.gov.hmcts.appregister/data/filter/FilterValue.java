package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * Binds together a keyable value that can be filtered.
 */
@Getter
public class FilterValue {
    private Keyable keyable;

    private Object value;

    public FilterValue(Keyable keyable) {
        this.keyable = keyable;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
