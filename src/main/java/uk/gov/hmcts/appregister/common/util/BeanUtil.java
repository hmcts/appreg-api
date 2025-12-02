package uk.gov.hmcts.appregister.common.util;

import org.springframework.beans.BeanUtils;

public class BeanUtil {
    /**
     * copies the bean. This is useful when you want to maintain the original state of the bean
     * before any updates are made to it. This is useful when using the Audit API {@link
     * uk.gov.hmcts.appregister.audit.service.AuditOperationService}
     *
     * @param beanToCopy The bean to copy
     * @return The bean copy
     */
    // CHECKSTYLE:OFF
    public static <T> T copyBean(T beanToCopy) {
        T before = null;
        if (beanToCopy != null) {
            before =
                    (T) org.springframework.beans.BeanUtils.instantiateClass(beanToCopy.getClass());
            BeanUtils.copyProperties(beanToCopy, before);
        }

        return before;
    }
}
