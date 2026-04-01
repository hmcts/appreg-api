package uk.gov.hmcts.appregister.data.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.util.CopyUtil;

/**
 * Binds together a keyable and a specific filter value related to that keyable.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FilterFieldValue<T extends Keyable> {
    private T keyable;

    private Object value;

    public FilterFieldValue(FilterFieldValue<T> filterValue) {
        setKeyable(CopyUtil.deepClone(filterValue.keyable));
        setValue(CopyUtil.deepClone(filterValue.value));
    }
}
