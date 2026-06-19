package uk.gov.hmcts.appregister.audit.listener.diff;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
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
 * <p>The class supports use of the {@link uk.gov.hmcts.appregister.audit.listener.diff.Audit} and
 * {@link uk.gov.hmcts.appregister.audit.listener.diff.AuditEnabled} annotations to tailor the way
 * in which it detects audit data.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ReflectiveAuditor implements Auditor {
    private record ProcessedMethodTarget(
            ReflectionCaches.MethodData methodData, int targetIdentityHash) {}

    private static final ClassValue<Set<CrudEnum>> AUDIT_ENABLED_TYPES_CACHE =
            new ClassValue<>() {
                @Override
                protected Set<CrudEnum> computeValue(Class<?> type) {
                    var auditEnabled = type.getAnnotation(AuditEnabled.class);
                    if (auditEnabled == null) {
                        return Collections.emptySet();
                    }

                    var auditTypes = EnumSet.noneOf(CrudEnum.class);
                    Collections.addAll(auditTypes, auditEnabled.types());
                    return auditTypes;
                }
            };

    private static final ClassValue<Map<Field, Set<CrudEnum>>> FIELD_AUDIT_ACTIONS_CACHE =
            new ClassValue<>() {
                @Override
                protected Map<Field, Set<CrudEnum>> computeValue(Class<?> type) {
                    var auditActionsByField = new HashMap<Field, Set<CrudEnum>>();

                    for (Field field : ReflectionCaches.getAllFields(type)) {
                        var audit = field.getAnnotation(Audit.class);
                        if (audit == null) {
                            continue;
                        }

                        var auditActions = EnumSet.noneOf(CrudEnum.class);
                        Collections.addAll(auditActions, audit.action());
                        auditActionsByField.put(field, auditActions);
                    }

                    return auditActionsByField;
                }
            };

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
                isAuditableAnnotatedForOperation(crudEnum, BeanUtil.getProxyClass(val)));

        return diffs;
    }

    private static void extractAuditData(
            CrudEnum crudEnum,
            Object val,
            List<AuditableData> differenceList,
            Set<ProcessedMethodTarget> processed,
            boolean recurseNestedObjects,
            boolean useAnnotations) {
        if (val != null) {

            for (ReflectionCaches.MethodData method :
                    ReflectionCaches.METHOD_CACHE.get(BeanUtil.getProxyClass(val)).methods()) {

                // if we are using annotations check if the method is annotated for this crud
                // operation
                // else ignore the method
                if (!useAnnotations
                        || (!isFieldAnnotatedForCrudAuditOperation(method.field(), crudEnum))) {
                    log.trace(
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
            Set<ProcessedMethodTarget> processed,
            boolean useAnnotations) {
        // if collection then iterate and compare contents, if not a collection then
        // process the complex objects
        // if we have object reursion turned on
        if (!isCollection(method.method().getReturnType()) && recurseNestedObjects) {
            log.trace("Method {}", method.method().getName());

            Object newValRet = invokeMethodForNew(method, val, processed);

            log.trace("New Value Ret {}", newValRet);

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
            Set<ProcessedMethodTarget> processed) {
        log.trace("Method {}", method.method().getName());

        Object valRet = val != null ? invokeMethodForNew(method, val, processed) : "";

        // if the value is null then set to empty string for comparison purposes
        String valueString = valRet != null ? valRet.toString() : "";

        log.trace("Value Ret {}", val);

        // detect diff
        log.trace(
                "Difference detected in field: {} value: {}",
                method.field().getName(),
                valueString);

        // store the difference knowing that new value is not null
        differenceList.add(new AuditableData(method.tableName(), method.columnName(), valueString));
    }

    private static Object invokeMethodForNew(
            ReflectionCaches.MethodData method,
            Object target,
            Set<ProcessedMethodTarget> processed) {
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
            ReflectionCaches.MethodData method,
            Object target,
            Set<ProcessedMethodTarget> processed) {
        if (target != null) {
            var processedKey = new ProcessedMethodTarget(method, System.identityHashCode(target));

            if (!processed.contains(processedKey)) {
                try {
                    Object m = method.method().invoke(target);
                    processed.add(processedKey);

                    log.trace("Processed {} on {}", method.method(), target);
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
        boolean isKeyable = Keyable.class.isAssignableFrom(type);
        log.trace("Is complex : {} {}", type, isKeyable);
        return isCollection(type) || isKeyable;
    }

    /**
     * Checks if this type is a collection i.e. list
     *
     * @param type The type to check
     * @return True or false
     */
    public static boolean isCollection(Class<?> type) {
        log.trace("Is collection : {}", type);
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
        return AUDIT_ENABLED_TYPES_CACHE.get(cls).contains(crudEnum);
    }

    /**
     * is the method annotated with the relevant crud audit operation.
     *
     * @param method The method to check
     * @param crudEnum The crud operation
     * @return true if annotated for the crud operation
     */
    public static boolean isFieldAnnotatedForCrudAuditOperation(Field method, CrudEnum crudEnum) {
        return FIELD_AUDIT_ACTIONS_CACHE
                .get(method.getDeclaringClass())
                .getOrDefault(method, Collections.emptySet())
                .contains(crudEnum);
    }
}
