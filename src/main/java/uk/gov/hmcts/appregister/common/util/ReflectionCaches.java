package uk.gov.hmcts.appregister.common.util;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A central place for cache to exist to avoid reflection based performance problems
 */
public class ReflectionCaches {
    public record MethodData(String tableName, String columnName, Method method) {}
    public record ReflectionMeta(List<MethodData> methods) {}

    /**
     * The method cache that is used for performance
     * @return The method cache for performance
     */
    public static final ClassValue<ReflectionMeta> METHOD_CACHE =
            new ClassValue<>() {
                @Override
                protected ReflectionMeta computeValue(Class<?> type) {
                    String table = getTableName(type);
                    List<MethodData> returnMethods = new java.util.ArrayList<>();
                    for (Field field : getAllFields(type)) {
                        Method get = getGetterForField(type, field.getName());
                        String col = getColumnOrJoinColumnName(field);
                       if (get != null && col!=null) {
                            returnMethods.add( new MethodData(table, col, get));
                        }
                    }

                    return new ReflectionMeta(returnMethods);
                }
            };

    /**
     * Returns all methods declared in the given class and its superclasses.
     * Includes private/protected/package methods, and avoids duplicates.
     */
    public static List<Field> getAllFields(Class<?> type) {
        List<Field> methods = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            methods.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return methods;
    }

    /**
     * Returns the getter method corresponding to a field if it exists.
     * Example: field 'status' → method 'getStatus' or 'isStatus' (for booleans)
     */
    public static Method getGetterForField(Class<?> clazz, String fieldName) {
        try {
            Field field = findField(clazz, fieldName);
            if (field == null) return null;

            String name = field.getName();
            Class<?> type = field.getType();

            // Build expected getter name
            String getterName;
            if (type == boolean.class || type == Boolean.class) {
                getterName = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            } else {
                getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }

            // Try to find the getter method
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            return null; // getter not defined
        }
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

    /**
     * Finds a field by name in the given class or its superclasses.
     *
     * @param type       the class to search
     * @param fieldName  the name of the field
     * @return the Field if found, otherwise null
     */
    public static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;

        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true); // allow access to private fields
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass(); // move up the hierarchy
            }
        }

        return null; // not found
    }

    /**
     * Returns the column name defined on a getter method via @Column or @JoinColumn.
     *
     * @param field the Java method to inspect (usually a getter)
     * @return the column name if present, otherwise null
     */
    public static String getColumnOrJoinColumnName(Field field) {
        if (field == null) return null;

        // Try @Column first
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isBlank()) {
            return column.name();
        }

        // Try @JoinColumn next
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null && !joinColumn.name().isBlank()) {
            return joinColumn.name();
        }

        // No annotation found
        return null;
    }
}
