package uk.gov.hmcts.appregister.common.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * A class that enforces the uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum codes are
 * consistent as well as unique across all enums on the classpath.
 *
 * <p>Instead of this test class, we could have implemented this as an annotation processor to
 * enforces these rules at compile time, but this class is simpler to maintain.
 */
public class CheckErrorCodeTest {

    private static final String DELINEATOR = "-";

    private static final String CODE_REGEX =
            "^[^" + DELINEATOR + "]+" + DELINEATOR + "[^" + DELINEATOR + "]+$";

    private static final Pattern CODE_PATTERN = Pattern.compile(CODE_REGEX);

    /**
     * checks that the error codes on the classpath have unique consistent numbering scheme across
     * all {@link uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum}} implementations.
     */
    @Test
    public void validateErrorEnums() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCodeEnum.class));
        Set<BeanDefinition> candidates =
                scanner.findCandidateComponents("uk.gov.hmcts.appregister");
        Assertions.assertFalse(candidates.isEmpty());

        List<String> errorCodesForAllClasses = new ArrayList<>();
        for (BeanDefinition bd : candidates) {
            Class<?> cls = Class.forName(bd.getBeanClassName());

            // check that all enums are enums
            Assertions.assertTrue(cls.isEnum());
            Object[] constants = cls.getEnumConstants();

            String prefix = null;

            // loop through all of error enum entries
            for (Object constant : constants) {
                ErrorCodeEnum operationStatus = (ErrorCodeEnum) constant;
                String appCode = operationStatus.getCode().getAppCode();

                // check that the error code has the correct format using regex
                boolean match = CODE_PATTERN.matcher(appCode).matches();
                Assertions.assertTrue(match);

                // if we dont have a prefix for this error code enum get one and use that to compare
                // against all others
                // to ensure consistency in defining app codes that exist in an enum
                if (prefix == null) {
                    prefix = getErrorCodeLetters(operationStatus.getCode().getAppCode());
                } else {
                    // make sure that all app code use the same code prefix
                    Assertions.assertEquals(
                            prefix,
                            getErrorCodeLetters(operationStatus.getCode().getAppCode()),
                            "Invalid prefix consistency for %s in class %s"
                                    .formatted(
                                            appCode,
                                            operationStatus.getClass().getCanonicalName()));
                }

                // check that the app code is not found any where else across all error code enums
                Assertions.assertFalse(
                        errorCodesForAllClasses.contains(appCode),
                        "Duplicate app code across classes found " + appCode);

                // add the code to a global list to check for duplicates
                errorCodesForAllClasses.add(appCode);
            }
        }
    }

    /**
     * gets the letters before the dash in the error code.
     *
     * @param code The code
     * @return The letters before the delimiter
     */
    private String getErrorCodeLetters(String code) {
        return code.split(DELINEATOR)[0];
    }
}
