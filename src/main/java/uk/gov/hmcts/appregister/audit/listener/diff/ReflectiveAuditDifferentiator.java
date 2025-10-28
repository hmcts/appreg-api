package uk.gov.hmcts.appregister.audit.listener.diff;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.ReflectionCaches;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * A generic reflective audit differentiator that can be used to compare two objects of any type for audit purposes
 *
 * If performance issues are a concern, consider implementing a specific differentiator for the object type.
 *
 * This class does uses a cache to mitigate the use of reflective performance issues where possible
 *
 * The class has build is recursion protection to avoid circular references. Any errors are not fatal
 * to the core operation of the business logic but will be logged.
 *
 * We can toggle recursion of nested objects and collection objects via the constructor parameters
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ReflectiveAuditDifferentiator implements AuditDifferentiator {

    /** Do we need to recurse nested objects */
    private final boolean recurseNestedObjects;

    /** Do we need to recurse collections*/
    private final boolean recurseCollectionObjects;

    /** Represents a null value. We default to a null string */
    public final static String EMPTY_VALUE= "null";

    @Override
    public boolean doesRecurseComplexObjects() {
        return recurseNestedObjects;
    }

    @Override
    public boolean doesRecurseCollectionObjects() {
        return recurseCollectionObjects;
    }

    @Override
    public List<Difference> diff(CRUDEnum crudEnum, Keyable oldVal, Keyable newVal) {
        return difference(crudEnum, oldVal, newVal, recurseNestedObjects, recurseCollectionObjects);
    }

    @Override
    public List<Difference> diff(CRUDEnum crudEnum, Keyable newVal) {
        return difference(crudEnum, null, newVal, recurseNestedObjects, recurseCollectionObjects);
    }

    /**
     * process the differences for the new value
     * @param oldVal The old value
     * @param newVal The new value
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param recurseCollectionObjects Whether we recurse into collection objects
    */
    public static List<Difference> difference(CRUDEnum crudEnum, Keyable oldVal, Keyable newVal, boolean recurseNestedObjects, boolean recurseCollectionObjects) {
        List<Difference> diffs = new ArrayList<>();

        if (oldVal == null && newVal == null) {
            log.debug("Two null values have been detected. No differences to process");
            return List.of();
        }

        // make sure if we are comparing old or new then the ids match
        if (newVal != null && oldVal != null && (newVal.getId() == null || !newVal.getId().equals(oldVal.getId()))) {
            log.debug("Expected the same key {} {}", newVal.getId(), oldVal.getId());
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot compare objects with different keys");
        }

        // make sure if we are comparing old or new then the types match
        if ((newVal != null && oldVal != null && !newVal.getClass().getCanonicalName().equals(oldVal.getClass().getCanonicalName()))) {
            log.debug("Expected the same key {} {}", newVal.getId(), oldVal.getId());
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot compare objects that are not the same type");
        }

        difference(crudEnum, oldVal, newVal, diffs, new HashSet<>(), recurseNestedObjects, recurseCollectionObjects, isAuditableAnnotated(newVal !=null ? newVal.getClass() : oldVal.getClass()));

        return diffs;
    }

    /**
     * process the differences for the new value against nothing i.e. all contents show up as a difference
     * @param newVal The new value
     * @param differenceList the captured differences
     * @param processed The processed method call and the objects that were invoked
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param recurseCollectionObjects Whether we recurse into collection objects
     */
    private static void  difference(CRUDEnum crudEnum, Object newVal, List<Difference> differenceList, Set<String> processed,
                                    boolean recurseNestedObjects, boolean recurseCollectionObjects) {
        difference(crudEnum, null, newVal, differenceList, processed, recurseNestedObjects,
                recurseCollectionObjects,  isAuditableAnnotated(newVal.getClass()));
    }

    /**
     * process the differences for the new value
     * @param oldVal The old value
     * @param newVal The new value
     * @param differenceList the captured differences
     * @param processed The processed method call and the objects that were invoked
     * @param useAnnotations Whether we should use annotations to determine what diferences to capture
     */
    private static void difference(CRUDEnum crudEnum, Object oldVal, Object newVal, List<Difference> differenceList, Set<String> processed,
                                   boolean recurseNestedObjects,
                                   boolean recurseCollectionObjects,
                                   boolean useAnnotations) {
            if (newVal != null || oldVal != null) {
                for (ReflectionCaches.MethodData method : ReflectionCaches.METHOD_CACHE.get(newVal != null
                        ? newVal.getClass() : oldVal.getClass()).methods()) {

                    // if we are using annotations check if the method is annotated for this crud operation
                    // else ignore the method
                    if (useAnnotations && !isFieldAnnotatedForCrudAuditOperation(method.field(), crudEnum)) {
                        log.debug("Skipping method {} as not annotated for {}", method.method().getName(), crudEnum);
                        continue;
                    }

                    // check if they are logical equivalent
                    if (!isComplexWrapper(method.method().getReturnType())) {
                        log.debug("Method {}", method.method().getName());

                        Object newValRet = newVal != null ? invokeMethodForNew(method, newVal, processed) : null;

                        log.debug("New Value Ret {}", newValRet);

                        Object oldValRet = oldVal != null ? invokeMethodForOld(method, oldVal, processed) : null;
                        log.debug("Old Value Ret {}", oldValRet);

                        // detect diff
                        if (newValRet != null && !newValRet.toString().equals(oldValRet!=null ? oldValRet.toString() : null)) {
                            log.debug("Difference detected {} in field new : {} and old{}", method.method().getName(),
                                    newVal, oldVal);

                            differenceList.add(new Difference(method.tableName(), method.columnName(), oldValRet != null ? oldValRet.toString() : EMPTY_VALUE,
                                    newValRet.toString()));
                        } else if (oldValRet != null && !oldValRet.toString().equals(newValRet!=null ? newValRet.toString() : null)) {
                            log.debug("Difference detected {} in field new : {} and old{}", method.method().getName(),
                                    newVal, oldVal);

                            differenceList.add(new Difference(method.tableName(), method.columnName(), oldValRet.toString(),
                                    newValRet != null ? newValRet.toString() : EMPTY_VALUE));
                        }
                    } else {

                        // if collection then iterate and compare contents
                        if (isCollection(method.method().getReturnType()) && recurseCollectionObjects) {
                            processListDiff(crudEnum, oldVal, newVal, differenceList, recurseNestedObjects,
                                     useAnnotations, processed, method);
                        }
                        // if a complex object then recurse
                        else if (recurseNestedObjects) {
                            log.debug("Method {}", method.method().getName());

                            Object newValRet = newVal != null ? invokeMethodForNew(method, newVal, processed) : null;

                            log.debug("New Value Ret {}", newValRet);

                            Object oldValRet = oldVal != null ? invokeMethodForOld(method, oldVal, processed) : null;
                            log.debug("Old Value Ret {}", oldValRet);

                            // recurse and get the differences in the complex object
                            difference(crudEnum, oldValRet, newValRet, differenceList, processed, recurseNestedObjects, recurseCollectionObjects, useAnnotations);
                        }
                    }
                }
            }
    }

    /**
     * processes the list difference between the old and new value objects
     * @param crudEnum The CRUD operation
     * @param oldVal The old value
     * @param newVal The new value
     * @param differenceList The list to build up
     * @param recurseNestedObjects Whether we recurse into nested objects
     * @param useAnnotations Whether we use annotations to determine what to audit
     * @param processed The processed set to avoid infinite recursion
     * @param method The method data to get the list
     */
    private static void processListDiff(CRUDEnum crudEnum, Object oldVal, Object newVal, List<Difference> differenceList,
                                    boolean recurseNestedObjects,
                                    boolean useAnnotations,
                                    Set<String> processed,
                                    ReflectionCaches.MethodData method) {
        List<?> newValRetLst = newVal != null ? (List<?>)invokeMethodForNew(method, newVal, processed) : null;
        List<?> oldValRetLst = oldVal != null ? (List<?>)invokeMethodForOld(method, oldVal, processed) : null;

        if ((newValRetLst !=null && oldValRetLst != null && newValRetLst.size() >= oldValRetLst.size()) || (newValRetLst!=null
                && oldValRetLst == null)) {
            for (int i = 0; i < newValRetLst.size(); i++) {
                boolean complex = isComplexWrapper(newValRetLst.get(i).getClass());
                if (complex && recurseNestedObjects) {
                    difference(crudEnum, getFromIndex(oldValRetLst, i), getFromIndex(newValRetLst, i), differenceList, processed, recurseNestedObjects, true, useAnnotations);
                } else if (!complex){
                    differenceList.add(new Difference(method.tableName(), method.columnName(), getFromIndex(oldValRetLst, i) != null ? getFromIndex(oldValRetLst, i).toString() : EMPTY_VALUE,
                            getFromIndex(newValRetLst, i) != null ? getFromIndex(newValRetLst, i).toString() : EMPTY_VALUE));
                }
            }
        } else if (oldValRetLst != null) {
            for (int i = 0; i < oldValRetLst.size(); i++) {
                boolean complex = isComplexWrapper(oldValRetLst.get(i).getClass());
                if (complex && recurseNestedObjects) {
                    difference(crudEnum, getFromIndex(oldValRetLst, i), getFromIndex(newValRetLst, i), differenceList, processed, recurseNestedObjects, true, useAnnotations);
                } else if (!complex){
                    differenceList.add(new Difference(method.tableName(), method.columnName(), getFromIndex(oldValRetLst, i).toString(),
                            getFromIndex(newValRetLst, i) != null ? getFromIndex(newValRetLst, i).toString() : EMPTY_VALUE));
                }
            }
        }
    }

    /**
     * gets an object from a list at the specified index safely and consume the associated exception
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

    private static Object invokeMethodForNew(ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        return invokeMethod("NEW_", method, target,processed);
    }

    private static Object invokeMethodForOld(ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        return invokeMethod("OLD_", method, target,processed);
    }

    /**
     * invokes a method and records its invocation against the target to avoid infinite recursion
     * @param prefix The prefix to use for the processed set
     * @param method The method to invoke
     * @param target The target object
     * @param processed The processed set to avoid infinite recursion
     */
    private static Object invokeMethod(String prefix, ReflectionCaches.MethodData method, Object target, Set<String> processed) {
        if (target!=null) {
            int hash = System.identityHashCode(target);

           if (!processed.contains(prefix + method.method() + hash)) {
               try {
                   Object m = method.method().invoke(target);
                   processed.add(prefix + method.method() + hash);

                   log.debug("Processed {} on {}", method.method(), target);
                   return m;
               }
               catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | SecurityException e)
               {
                   log.warn("Carrying on processing", e);
               }
            } else {
                log.debug("Already processed {} using {}", method.method(), target);
           }
        }
        return null;
    }

    /**
     * Checks if this type is complex i.e. a collection or a keyable object
     * @param type The type to check
     * @return True or false
     */
    public static boolean isComplexWrapper(Class<?> type) {
        log.debug("Is complex : {} {}",  type.toString(), Keyable.class.isAssignableFrom(type));
        return isCollection(type) || Keyable.class.isAssignableFrom(type);
    }

    /**
     * Checks if this type is a collection i.e. list
     * @param type The type to check
     * @return True or false
     */
    public static boolean isCollection(Class<?> type) {
        log.debug("Is complex : {} {}",  type.toString(), Keyable.class.isAssignableFrom(type));
        return List.class.isAssignableFrom(type);
    }

    /**
     * Is this an auditable class
     * @param cls The class to check
     * @return true if auditable
     */
    public static boolean isAuditableAnnotated(Class<?> cls) {
        return cls.getAnnotation(AuditEnabled.class) != null;
    }

    /**
     * is the method annotated with the relevant crud audit operation
     * @param method The method to check
     * @param crudEnum The crud operation
     * @return true if annotated for the crud operation
     */
    public static boolean isFieldAnnotatedForCrudAuditOperation(Field method, CRUDEnum crudEnum) {
        Audit audit = method.getAnnotation(Audit.class);
        return (audit != null && crudEnum == audit.action());
    }
}
