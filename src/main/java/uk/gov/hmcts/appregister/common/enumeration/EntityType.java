package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

/**
 * Enumeration representing the entity.
 */
@Getter
public enum EntityType {
    PERSON("Person"),
    ORGANISATION("Organisation"),
    UNKNOWN("Unknown");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }
}
