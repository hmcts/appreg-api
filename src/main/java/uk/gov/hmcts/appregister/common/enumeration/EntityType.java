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

    public static EntityType fromValue(String value) {
        for (EntityType party : values()) {
            if (party.value.equalsIgnoreCase(value)) {
                return party;
            }
        }
        throw new IllegalArgumentException("Unknown entity type: " + value);
    }
}
