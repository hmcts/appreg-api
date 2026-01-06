package uk.gov.hmcts.appregister.common.audit.listener.diff;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.util.ReflectionCaches;

/**
 * A generic reflective auditor that can be used get audit data from a {@link
 * uk.gov.hmcts.appregister.common.entity.base.Keyable} object.
 *
 * <p>If performance issues are a concern, consider implementing a specific differentiator
 * operation.
 *
 * <p>This class does uses a cache to mitigate the use of reflective performance issues where
 * possible
 *
 * <p>The class has build is recursion protection to avoid circular references. Any reflection
 * errors are not fatal to the core operation of the business logic but will be logged.
 *
 * <p>We can toggle recursion of nested objects via the constructor parameters.
 *
 * <p>The class supports use of the {@link Audit} and {@link AuditEnabled} annotations to tailor the
 * way in which it detects audit data.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ReflectiveAuditor implements Auditor {

    /** Do we need to recurse nested objects. */
    private final boolean recurseNestedObjects;

    @Override
    public boolean doesRecurseComplexObjects() {
        return recurseNestedObjects;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum, Keyable keyable) {
        return extractAuditData(crudEnum, keyable, recurseNestedObjects);
    }

    /**
     * process the audit data for the value.
     *
     * @param crudEnum The audit operation
     * @param val The value to get auidit data from
     * @param recurseNestedObjects Whether we recurse into nested objects
     */
    public static List<AuditableData> extractAuditData(
            CrudEnum crudEnum, Keyable val, boolean recurseNestedObjects) {
        final List<AuditableData> diffs = new ArrayList<>();

        extractAuditData(
                crudEnum,
                val,
                diffs,
                new HashSet<>(),
                recurseNestedObjects,
                isAuditableAnnotatedForOperation(crudEnum, val.getClass()));

        return diffs;
    }

    private static void extractAuditData(
            CrudEnum crudEnum,
            Object val,
            List<AuditableData> differenceList,
            Set<String> processed,
            boolean recurseNestedObjects,
            boolean useAnnotations) {
        if (val != null) {

            for (ReflectionCaches.MethodData method :
                    ReflectionCaches.METHOD_CACHE.get(getClassForType(val)).methods()) {

                // if we are using annotations check if the method is annotated for this crud
                // operation
                // else ignore the method
                if (useAnnotations
                        && !isFieldAnnotatedForCrudAuditOperation(method.field(), crudEnum)) {
                    log.debug(
                            "Skipping method {} as not annotated for {}",
                            method.method().getName(),
                            crudEnum);
                    continue;
                }

                // if the object is not complex wrap it
                if (!isComplexWrapper(method.method().getReturnType())) {
                    storeAuditDiffData(val, differenceList, method, processed);
                } else {
                    extractAuditDataFromComplex(
                            recurseNestedObjects,
                            method,
                            crudEnum,
                            val,
                            differenceList,
                            processed,
                            useAnnotations);
                }
            }
        }
    }

    /**
     * Ensure that we work with the real class behind any hibernate proxy. This applies if a entity
     * is lazy loading
     *
     * @param val The value to check
     * @return The class to use
     */
    private static Class<?> getClassForType(Object val) {
        if (val instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }

        return val.getClass();
    }

    /**
     * process the audit data from a complex value.
     *
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param method The method to get the complex value
     * @param crudEnum The crud operation
     * @param val The complex value to get audit data from
     * @param differenceList The list to build up the audit data
     * @param processed The processed list to avoid recursion
     * @param useAnnotations Wether to use annotations or not when processing the operation
     */
    private static void extractAuditDataFromComplex(
            boolean recurseNestedObjects,
            ReflectionCaches.MethodData method,
            CrudEnum crudEnum,
            Object val,
            List<AuditableData> differenceList,
            Set<String> processed,
            boolean useAnnotations) {
        // if collection then iterate and compare contents, if not a collection then
        // process the complex objects
        // if we have object reursion turned on
        if (!isCollection(method.method().getReturnType()) && recurseNestedObjects) {
            log.debug("Method {}", method.method().getName());

            Object newValRet = invokeMethodForNew(method, val, processed);

            log.debug("New Value Ret {}", newValRet);

            // recurse and get the differences in the complex object containing in the
            // list
            extractAuditData(
                    crudEnum,
                    newValRet,
                    differenceList,
                    processed,
                    recurseNestedObjects,
                    useAnnotations);
        }
    }

    /**
     * Gets a value and stores the audit difference.
     *
     * @param val The value to call using the method
     * @param differenceList The list to build up the audit data
     * @param method The method to get the value
     * @param processed The processed set to avoid infinite recursion
     */
    private static void storeAuditDiffData(
            Object val,
            List<AuditableData> differenceList,
            ReflectionCaches.MethodData method,
            Set<String> processed) {
        log.debug("Method {}", method.method().getName());

        Object valRet = val != null ? invokeMethodForNew(method, val, processed) : "";

        // if the value is null then set to empty string for comparison purposes
        String valueString = valRet != null ? valRet.toString() : "";

        log.debug("Value Ret {}", val);

        // detect diff
        log.debug(
                "Difference detected in field: {} value: {}",
                method.field().getName(),
                valueString);

        // store the difference knowing that new value is not null
        differenceList.add(new AuditableData(method.tableName(), method.columnName(), valueString));
    }

    private static Object invokeMethodForNew(
            ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        return invokeMethod(method, target, processed);
    }

    /**
     * invokes a method and records its invocation against the target to avoid infinite recursion.
     *
     * @param method The method to invoke
     * @param target The target object
     * @param processed The processed set to avoid infinite recursion
     */
    private static Object invokeMethod(
            ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        if (target != null) {
            int hash = System.identityHashCode(target);

            if (!processed.contains(method.method().toString() + hash)) {
                try {
                    Object m = method.method().invoke(target);
                    processed.add(method.method().toString() + hash);

                    log.debug("Processed {} on {}", method.method(), target);
                    return m;
                } catch (IllegalArgumentException
                        | IllegalAccessException
                        | InvocationTargetException
                        | SecurityException e) {
                    log.warn("Carrying on processing", e);
                }
            } else {
                log.warn("Already processed {} using {}", method.method(), target);
            }
        }
        return null;
    }

    /**
     * Checks if this type is complex i.e. a collection or a keyable object
     *
     * @param type The type to check
     * @return True or false
     */
    public static boolean isComplexWrapper(Class<?> type) {
        log.debug("Is complex : {} {}", type.toString(), Keyable.class.isAssignableFrom(type));
        return isCollection(type) || Keyable.class.isAssignableFrom(type);
    }

    /**
     * Checks if this type is a collection i.e. list
     *
     * @param type The type to check
     * @return True or false
     */
    public static boolean isCollection(Class<?> type) {
        log.debug("Is complex : {} {}", type.toString(), Keyable.class.isAssignableFrom(type));
        return List.class.isAssignableFrom(type);
    }

    /**
     * Is this an auditable class for the specific operation.
     *
     * @param crudEnum The audit operation taking place
     * @param cls The class to check
     * @return true if auditable
     */
    public static boolean isAuditableAnnotatedForOperation(CrudEnum crudEnum, Class<?> cls) {
        AuditEnabled auditEnabled = cls.getAnnotation(AuditEnabled.class);

        if (auditEnabled != null) {
            return Arrays.stream(auditEnabled.types()).toList().stream()
                    .anyMatch(t -> t.equals(crudEnum));
        }

        return false;
    }

    /**
     * is the method annotated with the relevant crud audit operation.
     *
     * @param method The method to check
     * @param crudEnum The crud operation
     * @return true if annotated for the crud operation
     */
    public static boolean isFieldAnnotatedForCrudAuditOperation(Field method, CrudEnum crudEnum) {
        Audit audit = method.getAnnotation(Audit.class);

        if (audit != null) {
            for (CrudEnum action : audit.action()) {
                if (crudEnum == action) {
                    return true;
                }
            }
        }

        return false;
    }
}
