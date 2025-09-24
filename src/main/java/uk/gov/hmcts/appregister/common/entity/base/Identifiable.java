package uk.gov.hmcts.appregister.common.entity.base;

/**
 * A nameable entity to ensure consistent method names for basic information across entities. This
 * is useful for a multitude of reasons e.g. ensuring standardisation for sorting purposes etc.
 */
public interface Identifiable {
    String DEFAULT_VALUE = "N/A";

    default String getCode() {
        return DEFAULT_VALUE;
    }

    default String getTitle() {
        return DEFAULT_VALUE;
    }

    default String getName() {
        return DEFAULT_VALUE;
    }

    default String getDescription() {
        return DEFAULT_VALUE;
    }
}
