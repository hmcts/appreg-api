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
}
