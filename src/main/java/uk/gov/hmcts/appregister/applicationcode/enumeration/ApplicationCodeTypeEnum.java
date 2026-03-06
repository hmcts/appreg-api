package uk.gov.hmcts.appregister.applicationcode.enumeration;

/**
 * An enumeration that represents the application code type.
 */
public enum ApplicationCodeTypeEnum {
    ENFORCEMENT_FINES("EF");

    private final String codePrefix;

    ApplicationCodeTypeEnum(String codePrefix) {
        this.codePrefix = codePrefix;
    }

    /**
     * does a code belong to an application code type.
     *
     * @param applicationCodeEnum The application code enum that will force the basis of the match.
     * @param codeToCheck The code to check a match.
     * @return True or false
     */
    public static boolean isMatching(
            ApplicationCodeTypeEnum applicationCodeEnum, String codeToCheck) {
        return codeToCheck.startsWith(applicationCodeEnum.codePrefix);
    }
}
