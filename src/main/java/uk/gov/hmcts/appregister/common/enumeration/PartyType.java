package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

/**
 * Enumeration representing the party.
 */
@Getter
public enum PartyType {
    APPLICANT("Applicant"),
    RESPONDENT("Respondent");

    private final String value;

    PartyType(String value) {
        this.value = value;
    }

    public static PartyType fromValue(String value) {
        for (PartyType party : values()) {
            if (party.value.equalsIgnoreCase(value)) {
                return party;
            }
        }
        throw new IllegalArgumentException("Unknown party type: " + value);
    }
}
