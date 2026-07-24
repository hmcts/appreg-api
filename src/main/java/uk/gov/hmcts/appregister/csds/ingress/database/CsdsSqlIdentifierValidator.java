package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class CsdsSqlIdentifierValidator {
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private CsdsSqlIdentifierValidator() {
        // Utility class.
    }

    public static String requireValid(String value, String description) {
        if (!StringUtils.hasText(value) || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL " + description + ": " + value);
        }
        return value;
    }
}
