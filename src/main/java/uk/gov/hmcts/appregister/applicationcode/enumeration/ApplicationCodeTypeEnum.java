package uk.gov.hmcts.appregister.applicationcode.enumeration;

/**
 * An enumeration that adds specific data to specific application code records.
 */
public enum ApplicationCodeTypeEnum {
    ENFORCEMENT_FINES("Enforcement Fines", "EF");

    private final String description;
    private final String codePrefix;

    ApplicationCodeTypeEnum(String codePrefix, String description) {
        this.codePrefix = codePrefix;
        this.description = description;
    }

    /**
     * is a code belong to an application code type.
     *
     * @param applicationCodeEnum The application code enum that will force the basis of the match.
     * @param codeToCheck The code to check a match.
     * @return True or false
     */
    public static boolean isMatching(
            ApplicationCodeTypeEnum applicationCodeEnum, String codeToCheck) {
        return applicationCodeEnum.codePrefix.startsWith(codeToCheck);
    }
}
