package uk.gov.hmcts.appregister.audit.listener.diff;

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
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.ReflectionCaches;

/**
 * A generic reflective audit differentiator that can be used to compare two {@link
 * uk.gov.hmcts.appregister.common.entity.base.Keyable} of any type for audit purposes
 *
 * <p>If performance issues are a concern, consider implementing a specific differentiator for the
 * object type.
 *
 * <p>This class does uses a cache to mitigate the use of reflective performance issues where
 * possible
 *
 * <p>The class has build is recursion protection to avoid circular references. Any errors are not
 * fatal to the core operation of the business logic but will be logged.
 *
 * <p>We can toggle recursion of nested objects and collection objects via the constructor
 * parameters.
 *
 * <p>The class supports use of the {@link uk.gov.hmcts.appregister.audit.listener.diff.Audit} and
 * {@link uk.gov.hmcts.appregister.audit.listener.diff.AuditEnabled} annotations to tailor the way
 * in which it detects differences between two {@link
 * uk.gov.hmcts.appregister.common.entity.base.Keyable} objects.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ReflectiveAuditDifferentiator implements AuditDifferentiator {

    /** Do we need to recurse nested objects. */
    private final boolean recurseNestedObjects;

    /** Do we need to recurse collections. */
    private final boolean recurseCollectionObjects;

    /** Represents a null value. We default to a null string. */
    public static final String EMPTY_VALUE = "null";

    @Override
    public boolean doesRecurseComplexObjects() {
        return recurseNestedObjects;
    }

    @Override
    public boolean doesRecurseCollectionObjects() {
        return recurseCollectionObjects;
    }

    @Override
    public List<Difference> diff(CrudEnum crudEnum, Keyable oldVal, Keyable newVal) {
        return difference(crudEnum, oldVal, newVal, recurseNestedObjects, recurseCollectionObjects);
    }

    @Override
    public List<Difference> diff(CrudEnum crudEnum, Keyable newVal) {
        return difference(crudEnum, null, newVal, recurseNestedObjects, recurseCollectionObjects);
    }

    /**
     * process the differences for the old and new value.
     *
     * @param oldVal The old value
     * @param newVal The new value
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param recurseCollectionObjects Whether we recurse into collection objects
     */
    public static List<Difference> difference(
            CrudEnum crudEnum,
            Keyable oldVal,
            Keyable newVal,
            boolean recurseNestedObjects,
            boolean recurseCollectionObjects) {
        final List<Difference> diffs = new ArrayList<>();

        if (oldVal == null && newVal == null) {
            log.debug("Two null values have been detected. No differences to process");
            return List.of();
        }

        // make sure if we are comparing old or new then the ids match
        if (newVal != null
                && oldVal != null
                && (newVal.getId() == null || !newVal.getId().equals(oldVal.getId()))) {
            log.debug("Expected the same key {} {}", newVal.getId(), oldVal.getId());
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot compare objects with different keys");
        }

        // make sure if we are comparing old or new then the types match
        if ((newVal != null
                && oldVal != null
                && !newVal.getClass()
                        .getCanonicalName()
                        .equals(oldVal.getClass().getCanonicalName()))) {
            log.debug("Expected the same key {} {}", newVal.getId(), oldVal.getId());
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot compare objects that are not the same type");
        }

        difference(
                crudEnum,
                oldVal,
                newVal,
                diffs,
                new HashSet<>(),
                recurseNestedObjects,
                recurseCollectionObjects,
                isAuditableAnnotatedForOperation(
                        crudEnum, newVal != null ? newVal.getClass() : oldVal.getClass()));

        return diffs;
    }

    /**
     * process the differences for the new value against nothing i.e. all contents show up as a
     * difference
     *
     * @param newVal The new value
     * @param differenceList the captured differences
     * @param processed The processed method call and the objects that were invoked
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param recurseCollectionObjects Whether we recurse into collection objects
     */
    private static void difference(
            CrudEnum crudEnum,
            Object newVal,
            List<Difference> differenceList,
            Set<String> processed,
            boolean recurseNestedObjects,
            boolean recurseCollectionObjects) {
        difference(
                crudEnum,
                null,
                newVal,
                differenceList,
                processed,
                recurseNestedObjects,
                recurseCollectionObjects,
                isAuditableAnnotatedForOperation(crudEnum, newVal.getClass()));
    }

    /**
     * process the differences for the new value.
     *
     * @param oldVal The old value
     * @param newVal The new value
     * @param differenceList the captured differences
     * @param processed The processed method call and the objects that were invoked
     * @param useAnnotations Whether we should use annotations to determine what diferences to
     *     capture
     */
    private static void difference(
            CrudEnum crudEnum,
            Object oldVal,
            Object newVal,
            List<Difference> differenceList,
            Set<String> processed,
            boolean recurseNestedObjects,
            boolean recurseCollectionObjects,
            boolean useAnnotations) {
        if (newVal != null || oldVal != null) {

            // loop through all methods of the objects being passed
            for (ReflectionCaches.MethodData method :
                    ReflectionCaches.METHOD_CACHE
                            .get(newVal != null ? newVal.getClass() : oldVal.getClass())
                            .methods()) {

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
                    storeDifference(oldVal, newVal, differenceList, method, processed);
                } else {

                    // if collection then iterate and compare contents, if not a collection then
                    // process the complex objects
                    // if we have object reursion turned on
                    if (isCollection(method.method().getReturnType()) && recurseCollectionObjects) {
                        processListDiff(
                                crudEnum,
                                oldVal,
                                newVal,
                                differenceList,
                                recurseNestedObjects,
                                useAnnotations,
                                processed,
                                method);
                    } else if (recurseNestedObjects) {
                        log.debug("Method {}", method.method().getName());

                        Object newValRet =
                                newVal != null
                                        ? invokeMethodForNew(method, newVal, processed)
                                        : null;

                        log.debug("New Value Ret {}", newValRet);

                        Object oldValRet =
                                oldVal != null
                                        ? invokeMethodForOld(method, oldVal, processed)
                                        : null;
                        log.debug("Old Value Ret {}", oldValRet);

                        // recurse and get the differences in the complex object containing in the
                        // list
                        difference(
                                crudEnum,
                                oldValRet,
                                newValRet,
                                differenceList,
                                processed,
                                recurseNestedObjects,
                                recurseCollectionObjects,
                                useAnnotations);
                    }
                }
            }
        }
    }

    /**
     * Gets a value and stores the difference if detected between the old and new value.
     *
     * @param oldVal The old value to call using the method
     * @param newVal The new value to call using the method
     * @param differenceList The list to build up
     * @param method The method to get the new and old values
     * @param processed The processed set to avoid infinite recursion
     */
    private static void storeDifference(
            Object oldVal,
            Object newVal,
            List<Difference> differenceList,
            ReflectionCaches.MethodData method,
            Set<String> processed) {
        log.debug("Method {}", method.method().getName());

        Object newValRet = newVal != null ? invokeMethodForNew(method, newVal, processed) : null;

        log.debug("New Value Ret {}", newValRet);

        Object oldValRet = oldVal != null ? invokeMethodForOld(method, oldVal, processed) : null;
        log.debug("Old Value Ret {}", oldValRet);

        // detect diff
        storeDifference(oldValRet, newValRet, differenceList, method);
    }

    /**
     * Stores the difference if detected between the old and new value. This method works out if the
     * new or old value is null and compensates accordingly
     *
     * @param oldValRet The old value
     * @param newValRet The new value
     * @param differenceList The list to build up
     * @param method The method that was used to get the values
     */
    private static void storeDifference(
            Object oldValRet,
            Object newValRet,
            List<Difference> differenceList,
            ReflectionCaches.MethodData method) {
        // detect diff
        if (newValRet != null
                && !newValRet.toString().equals(oldValRet != null ? oldValRet.toString() : null)) {
            log.debug("Difference detected {} in field old: {} new: {}", newValRet, oldValRet);

            // store the difference knowing that new value is not null
            differenceList.add(
                    new Difference(
                            method.tableName(),
                            method.columnName(),
                            oldValRet != null ? oldValRet.toString() : EMPTY_VALUE,
                            newValRet.toString()));
        } else if (oldValRet != null
                && !oldValRet.toString().equals(newValRet != null ? newValRet.toString() : null)) {
            log.debug("Difference detected {} in field old: {} new: {}", newValRet, oldValRet);

            // store the difference knowing that old value is not null
            differenceList.add(
                    new Difference(
                            method.tableName(),
                            method.columnName(),
                            oldValRet.toString(),
                            newValRet != null ? newValRet.toString() : EMPTY_VALUE));
        }
    }

    /**
     * processes the list difference between the old and new value objects. This method selects
     * which list is prioritised i.e. if the enew list is larger than the old list we iterate on the
     * new list
     *
     * @param crudEnum The CRUD operation
     * @param oldVal The old value
     * @param newVal The new value
     * @param differenceList The list to build up
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param useAnnotations Whether we use annotations to determine what to audit
     * @param processed The processed set to avoid infinite recursion
     * @param method The method data to get the list data
     */
    private static void processListDiff(
            CrudEnum crudEnum,
            Object oldVal,
            Object newVal,
            List<Difference> differenceList,
            boolean recurseNestedObjects,
            boolean useAnnotations,
            Set<String> processed,
            ReflectionCaches.MethodData method) {
        List<?> newValRetLst =
                newVal != null ? (List<?>) invokeMethodForNew(method, newVal, processed) : null;
        List<?> oldValRetLst =
                oldVal != null ? (List<?>) invokeMethodForOld(method, oldVal, processed) : null;

        // if the new list is present and it is larger than or equal to the old list then iterate
        // the new list
        if ((newValRetLst != null
                        && oldValRetLst != null
                        && newValRetLst.size() >= oldValRetLst.size())
                || (newValRetLst != null && oldValRetLst == null)) {
            log.debug("Working with new list size {}", newValRetLst);

            processListDiff(
                    crudEnum,
                    newValRetLst,
                    oldValRetLst,
                    newValRetLst,
                    processed,
                    method,
                    differenceList,
                    recurseNestedObjects,
                    useAnnotations);
        } else if (oldValRetLst != null) {
            log.debug("Working with old list size {}", newValRetLst);
            processListDiff(
                    crudEnum,
                    oldValRetLst,
                    oldValRetLst,
                    newValRetLst,
                    processed,
                    method,
                    differenceList,
                    recurseNestedObjects,
                    useAnnotations);
        }
    }

    /**
     * processes the list difference between the old and new value objects.
     *
     * @param crudEnum The CRUD operation
     * @param lstToTraverse The list to traverse
     * @param oldValRetLst The old value list
     * @param newValRetLst The new value list
     * @param processed The processed
     * @param method The method data to get the list
     */
    private static void processListDiff(
            CrudEnum crudEnum,
            List<?> lstToTraverse,
            List<?> oldValRetLst,
            List<?> newValRetLst,
            Set<String> processed,
            ReflectionCaches.MethodData method,
            List<Difference> differenceList,
            boolean recurseNestedObjects,
            boolean useAnnotations) {

        // loop through all values in the list, comparing each in turn
        for (int i = 0; i < lstToTraverse.size(); i++) {
            log.debug("Looping through list index {} {}", i, lstToTraverse.get(i));
            boolean complex = isComplexWrapper(lstToTraverse.get(i).getClass());

            // if the list contains a complex value then iterate and process the values, otherwise
            // store the difference
            // based on a difference
            if (complex && recurseNestedObjects) {
                log.debug(
                        "Complex object detected and recursion enabled {} {}",
                        i,
                        lstToTraverse.get(i).getClass());
                difference(
                        crudEnum,
                        getFromIndex(oldValRetLst, i),
                        getFromIndex(newValRetLst, i),
                        differenceList,
                        processed,
                        recurseNestedObjects,
                        true,
                        useAnnotations);
            } else if (!complex) {
                log.debug("Simple object detected {} {}", i, lstToTraverse.get(i).getClass());
                storeDifference(
                        getFromIndex(oldValRetLst, i),
                        getFromIndex(newValRetLst, i),
                        differenceList,
                        method);
            }
        }
    }

    /**
     * gets an object from a list at the specified index safely and consume the associated
     * exception.
     *
     * @param list The list to get the object from
     * @param index The index to get
     * @return The object value or null
     */
    private static Object getFromIndex(List<?> list, int index) {
        if (list != null && list.size() > index) {
            try {
                return list.get(index);
            } catch (IndexOutOfBoundsException e) {
                log.debug("Index out of bounds {} for list size {}", index, list.size());
            }
        }
        return null;
    }

    private static Object invokeMethodForNew(
            ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        return invokeMethod("NEW_", method, target, processed);
    }

    private static Object invokeMethodForOld(
            ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        return invokeMethod("OLD_", method, target, processed);
    }

    /**
     * invokes a method and records its invocation against the target to avoid infinite recursion.
     *
     * @param prefix The prefix to use for the processed set
     * @param method The method to invoke
     * @param target The target object
     * @param processed The processed set to avoid infinite recursion
     */
    private static Object invokeMethod(
            String prefix,
            ReflectionCaches.MethodData method,
            Object target,
            Set<String> processed) {
        if (target != null) {
            int hash = System.identityHashCode(target);

            if (!processed.contains(prefix + method.method() + hash)) {
                try {
                    Object m = method.method().invoke(target);
                    processed.add(prefix + method.method() + hash);

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
