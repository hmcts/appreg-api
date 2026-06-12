package uk.gov.hmcts.appregister.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ReflectionCachesTest {

    @Test
    void methodCache_extractsMetadataForAnnotatedFields() {
        var meta = ReflectionCaches.METHOD_CACHE.get(ChildEntity.class);

        assertEquals(3, meta.methods().size());
        assertEquals("child_table", meta.methods().getFirst().tableName());
        assertNotNull(meta.methods().getFirst().method());
        assertNotNull(meta.methods().getFirst().field());
    }

    @Test
    void getAllFields_includesSuperclassFields() {
        var fields = ReflectionCaches.getAllFields(ChildEntity.class);

        assertTrue(fields.stream().anyMatch(field -> field.getName().equals("parentField")));
        assertTrue(fields.stream().anyMatch(field -> field.getName().equals("joinedField")));
    }

    @Test
    void getGetterForField_supportsBooleanAndStandardGetters() {
        assertEquals(
                "getJoinedField",
                ReflectionCaches.getGetterForField(ChildEntity.class, "joinedField").getName());
        assertEquals(
                "isActive",
                ReflectionCaches.getGetterForField(ChildEntity.class, "active").getName());
        assertNull(ReflectionCaches.getGetterForField(ChildEntity.class, "missing"));
    }

    @Test
    void getTableName_returnsAnnotationOrFallback() {
        assertEquals("child_table", ReflectionCaches.getTableName(ChildEntity.class));
        assertEquals("Table not defined", ReflectionCaches.getTableName(UnannotatedEntity.class));
    }

    @Test
    void findField_walksSuperclassHierarchy() {
        Field parentField = ReflectionCaches.findField(ChildEntity.class, "parentField");

        assertNotNull(parentField);
        assertEquals("parentField", parentField.getName());
        assertNull(ReflectionCaches.findField(ChildEntity.class, "missing"));
    }

    @Test
    void getColumnOrJoinColumnName_returnsExpectedValue() {
        assertEquals(
                "parent_column",
                ReflectionCaches.getColumnOrJoinColumnName(
                        ReflectionCaches.findField(ChildEntity.class, "parentField")));
        assertEquals(
                "joined_column",
                ReflectionCaches.getColumnOrJoinColumnName(
                        ReflectionCaches.findField(ChildEntity.class, "joinedField")));
        assertEquals(
                "Column Name not defined",
                ReflectionCaches.getColumnOrJoinColumnName(
                        ReflectionCaches.findField(ChildEntity.class, "active")));
        assertNull(ReflectionCaches.getColumnOrJoinColumnName(null));
    }

    static class ParentEntity {
        @Column(name = "parent_column")
        private String parentField;

        public String getParentField() {
            return parentField;
        }
    }

    @Table(name = "child_table")
    static class ChildEntity extends ParentEntity {
        @JoinColumn(name = "joined_column")
        private String joinedField;

        private boolean active;

        public String getJoinedField() {
            return joinedField;
        }

        public boolean isActive() {
            return active;
        }
    }

    static class UnannotatedEntity {
        // no-op
    }
}
