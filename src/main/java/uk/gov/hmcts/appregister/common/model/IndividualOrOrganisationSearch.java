package uk.gov.hmcts.appregister.common.model;

import lombok.RequiredArgsConstructor;

/**
 * A representation of the applicants name that the ability to split it into
 * forename and surname. Useful when services need to distinguish between
 * parts of a name from a plain string
 */
@RequiredArgsConstructor
public class IndividualOrOrganisationSearch {
    private final String name;

    private String individualNameOrOrganisationName;

    private Boolean hasIndividualSurname;

    private String individualSurname;

    /** A way of determining the fore and surname of an applicant */
    public static final String CHARACTER_TO_SPLIT_SURNAME = ",";

    public static IndividualOrOrganisationSearch of(String name) {
        return new IndividualOrOrganisationSearch(name);
    }

    /**
     * Does surname exist boolean.
     * @return Does the surname exist
     */
    public boolean doesSurNameExist() {
        if(hasIndividualSurname!=null) {
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
            individualNameOrOrganisationName = split[0].length() == 0 ? null : split[0].trim();
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
            individualSurname = parts[1].length() == 0 ? null : parts[1].trim();
        } else {
            return null;
        }

        return individualSurname;
    }
}
