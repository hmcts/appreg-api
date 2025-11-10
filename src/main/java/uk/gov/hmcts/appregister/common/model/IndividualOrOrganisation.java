package uk.gov.hmcts.appregister.common.model;

import lombok.RequiredArgsConstructor;

/**
 * A representation of the applicants name that the ability to split it into forename and surname.
 * Useful when services need to distinguish between parts of a name from a plain string
 */
@RequiredArgsConstructor
public class IndividualOrOrganisation {
    private final String name;

    private String individualNameOrOrganisationName;

    private Boolean hasIndividualSurname;

    private String individualSurname;

    /** The default name if parts are of the name are null. */
    public static String DEFAULT_NAME = "N/K";

    /** A way of determining the fore and surname of an applicant. */
    public static final String CHARACTER_TO_SPLIT_SURNAME = ",";

    public static IndividualOrOrganisation of(String name) {
        return new IndividualOrOrganisation(name);
    }

    /**
     * Does surname exist boolean.
     *
     * @return Does the surname exist
     */
    public boolean doesSurNameExist() {
        if (hasIndividualSurname != null) {
            return hasIndividualSurname;
        }

        String[] names = name.split(CHARACTER_TO_SPLIT_SURNAME);
        hasIndividualSurname = names.length == 2;
        return hasIndividualSurname;
    }

    public String getName() {
        if (name == null) {
            return null;
        }

        if (individualNameOrOrganisationName != null) {
            return individualNameOrOrganisationName;
        }

        String[] split = name.split(CHARACTER_TO_SPLIT_SURNAME);
        if (split.length > 0) {
            individualNameOrOrganisationName = split[0].isEmpty() ? null : split[0].trim();
            return individualNameOrOrganisationName;
        }
        return "";
    }

    public String getSurname() {
        if (name == null) {
            return null;
        }

        if (individualSurname != null) {
            return individualSurname;
        }

        String[] parts = name.split(CHARACTER_TO_SPLIT_SURNAME);
        if (parts.length > 1) {
            individualSurname = parts[1].isEmpty() ? null : parts[1].trim();
        } else {
            return null;
        }

        return individualSurname;
    }

    /**
     * gets the individual name string from forename and surname.
     *
     * @param forename The forename
     * @param surname The surname
     * @return The name string
     */
    public static String getIndividualNameString(String forename, String surname) {
        if (surname != null && !surname.isBlank()) {
            if (forename != null && !forename.isBlank()) {
                return forename.trim() + " " + surname.trim();
            } else {
                return DEFAULT_NAME + " " + surname.trim();
            }
        } else if (forename != null && !forename.isBlank()) {
            return forename.trim() + " " + DEFAULT_NAME;
        } else {
            return DEFAULT_NAME + " " + DEFAULT_NAME;
        }
    }
}
