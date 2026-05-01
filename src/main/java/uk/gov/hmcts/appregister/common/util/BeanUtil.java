package uk.gov.hmcts.appregister.common.util;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.BeanUtils;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;

public class BeanUtil {
    /**
     * copies the bean. This is useful when you want to maintain the original state of the bean
     * before any updates are made to it. This is useful when using the Audit API {@link
     * AuditOperationService}
     *
     * <p>This method handles Hibernate proxies by instantiating the underlying class.
     *
     * @param beanToCopy The bean to copy
     * @return The bean copy
     */
    // CHECKSTYLE:OFF
    public static <T> T copyBean(T beanToCopy) {
        T before = null;
        if (beanToCopy != null) {
            Class<?> classToInstantiate = getProxyClass(beanToCopy);

            before = (T) org.springframework.beans.BeanUtils.instantiateClass(classToInstantiate);
            BeanUtils.copyProperties(beanToCopy, before);
        }

        return before;
    }

    /**
     * gets the underlying class of a hibernate proxy or the class itself. This is useful when we
     * need to have reflective access to the values of a class as opposed to the proxy. This is
     * useful when copying data. See {@link #copyBean(Object)}
     *
     * @param bean The bean to check. Typically a hibernate proxy but can be any class
     * @return The class of the underlying bean or the class that was passed in if not a proxy
     */
    public static Class<?> getProxyClass(Object bean) {
        if (bean instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        } else {
            return bean.getClass();
        }
    }
}
