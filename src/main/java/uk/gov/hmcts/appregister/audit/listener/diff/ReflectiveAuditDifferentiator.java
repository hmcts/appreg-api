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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A generic reflective audit differentiator that can be used to compare two objects of any type
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
        return difference(o, t1, diffs);
    }

    @Override
    public List<Difference> diff(Keyable t1) {
        return difference(null, t1);
    }

    public static List<Difference> difference(Keyable newVal, Keyable oldVal) {
        List<Difference> diffs = new ArrayList<>();
        return difference(newVal, oldVal, diffs);
    }

    private static List<Difference> difference(Object newVal, Object oldVal, List<Difference> differenceList) {
        try {
            if (newVal.equals(oldVal)) {
                log.debug("No differences detected between new and old values {} ana {}", newVal, oldVal);
                // no differentces
                return differenceList;
            }

            for (Method method : ReflectionCaches.METHOD_CACHE.get(newVal.getClass()).methods()) {
                if (isAReturnMethod(method)) {
                    Object newValRet = method.invoke(newVal, (Object) null);
                    Object oldValRet = method.invoke(oldVal, (Object) null);

                    // check if they are logical equivalent
                    if (isPrimitiveOrWrapper(newValRet.getClass()) && !newValRet.equals(oldValRet)) {
                        log.debug("Difference detected {} in field new : {} and old{}", method.getName(),
                                newVal, oldVal);

                        // difference detected
                        differenceList.add( new Difference(getTableName(newVal.getClass()), getColumnOrJoinColumnName(method), oldValRet.toString(), newValRet.toString()));
                    } else {
                        // recurse and get the differences in the complex object
                        difference(newValRet, oldVal, differenceList);
                    }
                }
            }
            return differenceList;
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | SecurityException e) {
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR,
                    "Problem when establishing difference between objects", e);
        }
    }

    private static boolean isAReturnMethod(Method method) {
        return method.getParameters().length == 0 && !method.getReturnType().equals(Void.class);
    }

    /**
     * Returns the column name defined on a getter method via @Column or @JoinColumn.
     *
     * @param method the Java method to inspect (usually a getter)
     * @return the column name if present, otherwise null
     */
    public static String getColumnOrJoinColumnName(Method method) {
        if (method == null) return null;

        // Try @Column first
        Column column = method.getAnnotation(Column.class);
        if (column != null && !column.name().isBlank()) {
            return column.name();
        }

        // Try @JoinColumn next
        JoinColumn joinColumn = method.getAnnotation(JoinColumn.class);
        if (joinColumn != null && !joinColumn.name().isBlank()) {
            return joinColumn.name();
        }

        // No annotation found
        return "Column not defined";
    }

    /**
     * Returns the column name defined on a getter method via @Column or @JoinColumn.
     *
     * @param method the Java method to inspect (usually a getter)
     * @return the column name if present, otherwise null
     */
    public  static <T> String getTableName(Class<T> method) {
        // Try @JoinColumn next
        Table table = method.getAnnotation(Table.class);
        if (table != null) {
            return table.name();
        }

        // No annotation found
        return "Table not defined";
    }

    record ReflectionMeta(Method[] methods) {}

    private static final Set<Class<?>> WRAPPER_TYPES = Set.of(
            Boolean.class, Byte.class, Character.class,
            Short.class, Integer.class, Long.class,
            Float.class, Double.class, Void.class
    );

    public static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || WRAPPER_TYPES.contains(type);
    }
}
