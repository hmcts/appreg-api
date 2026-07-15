package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.List;
import java.util.Map;

public interface IngressDatabaseRowMapper<T> {
    List<String> columns();

    default List<String> updatableColumns() {
        return columns().subList(1, columns().size());
    }

    Map<String, String> insertExpressions();

    default Map<String, String> updateExpressions() {
        return insertExpressions();
    }

    Map<String, Object> toRow(T item);
}
