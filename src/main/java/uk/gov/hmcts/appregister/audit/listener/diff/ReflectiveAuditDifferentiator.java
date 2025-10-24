package uk.gov.hmcts.appregister.audit.listener.diff;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.ReflectionCaches;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A generic reflective audit differentiator that can be used to compare two objects of any type for audit purposes
 *
 * If performance issues are a concern, consider implementing a specific differentiator for the object type.
 *
 * This class uses a cache to mitigate the use of reflective performance issues where possible
 */
@Slf4j
@Component
public class ReflectiveAuditDifferentiator implements AuditDifferentiator {

    @Override
    public List<Difference> diff(Keyable o, Keyable t1) {
        List<Difference> diffs = new ArrayList<>();
        difference(o, t1, diffs, new HashSet<>());
        return diffs;
    }

    @Override
    public List<Difference> diff(Keyable t1) {
        List<Difference> diffs = new ArrayList<>();
        difference( t1, diffs, new HashSet<>());
        return diffs;
    }

    public static List<Difference> difference(Keyable newVal, Keyable oldVal) {
        List<Difference> diffs = new ArrayList<>();
        if (!newVal.getId().equals(oldVal.getId())) {
            log.debug("Expected the same key {} {}", newVal.getId(), oldVal.getId());
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot compare objects with different keys");
        }

        if (!newVal.getClass().getCanonicalName().equals(oldVal.getClass().getCanonicalName())) {
            log.debug("Expected the same key {} {}", newVal.getId(), oldVal.getId());
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot compare objects that are not the same type");
        }

        if (oldVal == null && newVal != null) {
            // old value is null so use the new value as a basis of diff
            difference(newVal, diffs, new HashSet<>());
        } if (newVal == null && oldVal != null) {
            // new value is null so use the old value as a basis of diff
            difference(oldVal, diffs, new HashSet<>());
        } else if (newVal != null && oldVal != null) {
            difference(newVal, oldVal, diffs, new HashSet<>());
        }

        return diffs;
    }

    private static void  difference(Object newVal, List<Difference> differenceList, Set<String> processed) {
        try {
            if (newVal != null) {
                for (ReflectionCaches.MethodData method : ReflectionCaches.METHOD_CACHE.get(newVal.getClass()).methods()) {
                    // record all new data as we dont have a an old to compare against
                    Object newValRet = invokeMethod(method, newVal, processed);

                    if (!isComplexWrapper(method.method().getReturnType())) {
                        log.debug("Difference detected {} in field new : {}", method.method().getName(),
                                newVal);
                        differenceList.add(new Difference(method.tableName(), method.columnName(), "null", newValRet != null ? newValRet.toString() : "null"));
                    } else {
                        // recurse and get the differences in the complex object
                        difference(newValRet, null, differenceList, processed);
                    }
                }
            }
        }
        catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | SecurityException e)
        {
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Problem when establishing difference between objects", e);
        }
    }


        private static void difference(Object newVal, Object oldVal, List<Difference> differenceList, Set<String> processed) {
        try {
                if (newVal != null || oldVal != null) {
                    for (ReflectionCaches.MethodData method : ReflectionCaches.METHOD_CACHE.get(newVal != null ? newVal.getClass() : oldVal.getClass()).methods()) {

                        log.debug("Method {}", method.method().getName());

                        Object newValRet = newVal != null ? invokeMethod(method, newVal, processed) : null;

                        log.debug("New Value Ret {}", newValRet);

                        Object oldValRet = oldVal != null ? invokeMethod(method, oldVal, processed) : null;
                        log.debug("Old Value Ret {}", oldValRet);

                        // check if they are logical equivalent
                        if (!isComplexWrapper(method.method().getReturnType())) {
                            log.debug("Difference detected {} in field new : {} and old{}", method.method().getName(),
                                    newVal, oldVal);

                            // detect diff
                            if (newValRet != null && !newValRet.equals(oldValRet)) {
                                differenceList.add(new Difference(method.tableName(), method.columnName(), oldValRet != null ? oldValRet.toString() : "null",
                                        newValRet.toString()));
                            } else if (oldValRet != null && !oldValRet.equals(newValRet)) {
                                differenceList.add(new Difference(method.tableName(), method.columnName(), oldValRet.toString(),
                                        newValRet != null ? newValRet.toString() : "null"));
                            }
                        } else {
                            // recurse and get the differences in the complex object
                            difference(newValRet, oldValRet, differenceList, processed);
                        }
                    }
                }
        }
        catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | SecurityException e)
        {
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Problem when establishing difference between objects", e);
        }
    }

    private static Object invokeMethod(ReflectionCaches.MethodData method, Object target, Set<String> processed) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (target!=null) {
           if (!processed.contains(method.method() + Integer.valueOf(System.identityHashCode(target)).toString())) {
                Object m = method.method().invoke(target);
                processed.add(method.method() + target.toString());

               log.debug("Processed {} on {}", method.method(), target);
                return m;
            } else {
                log.debug("Already processed {} on {}", method.method(), target);
           }
        }
        return null;
    }

    public static boolean isComplexWrapper(Class<?> type) {
        log.debug("Is complex : {} {}",  type.toString(), Keyable.class.isAssignableFrom(type));
        return Keyable.class.isAssignableFrom(type);
    }
}
