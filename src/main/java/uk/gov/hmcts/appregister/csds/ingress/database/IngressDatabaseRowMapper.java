package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.List;
import java.util.Map;

public interface IngressDatabaseRowMapper<T> {
    List<String> columns();

    List<String> updatableColumns();

    Map<String, String> insertExpressions();

    Map<String, String> updateExpressions();

    Map<String, Object> toRow(T item);
}
