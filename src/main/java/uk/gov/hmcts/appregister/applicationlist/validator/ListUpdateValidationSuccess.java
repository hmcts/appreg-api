package uk.gov.hmcts.appregister.applicationlist.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
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

    public ApplicationList getCopyOfApplicationList() {
        ApplicationList before = null;
        if (applicationList != null) {
            before = org.springframework.beans.BeanUtils.instantiateClass(ApplicationList.class);
            BeanUtils.copyProperties(applicationList, before);
        }

        return before;
    }
}
