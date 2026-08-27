package uk.gov.hmcts.appregister.common.template;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

class WordingTemplateTest {
    private static final String DATE_TEMPLATE =
            "This is a test {DATE|Applicant officer|10} with a date";

    private static final String TEXT_TEMPLATE2 =
            "This is a test {TEXT|Applicant officer|10} with a date";

    @Test
    void testTemplateAllDataTypeDefaultsToText() {
        WordingTemplateSentence.WordingTemplate template =
                WordingTemplateSentence.WordingTemplate.with("{Unknown|Applicant officer|10}");
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT, template.getDetail().getConstraint().getType());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{Wrong}",
                "{Wrong",
                "{TEXT|Applicant officer|10|EXTRA}",
                "{|Applicant officer|10}",
                "{TEXT||10}",
                "{TEXT|Applicant officer|}"
            })
    void testTemplateFailParsingTemplateFormatIncorrect(String invalidTemplate) {
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> WordingTemplateSentence.WordingTemplate.with(invalidTemplate));
        Assertions.assertEquals(
                CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, appRegistryException.getCode());
    }

    @Test
    void testParameterSizeMismatch() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(DATE_TEMPLATE);
        List<TemplateSubstitution> substitutions = List.of();
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> collection.substitute(substitutions));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    @Test
    void testInvalidLengthFormatFailure() {
        var collection = WordingTemplateSentence.with(TEXT_TEMPLATE2);
        var templateable = collection.getTemplateableContents()[0];
        var appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                collection.substituteForTemplate(
                                        templateable, "this value exceeds length"));
        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE, appRegistryException.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"left{brace", "right}brace", "carriage\rreturn", "line\nbreak"})
    void givenReservedCharacterWhenSubstitutingThenRejectWithoutEchoingValue(String value) {
        var collection = WordingTemplateSentence.with(TEXT_TEMPLATE2);
        var exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                collection.substituteForTemplate(
                                        collection.getTemplateableContents()[0], value));

        Assertions.assertEquals(CommonAppError.WORDING_DATA_TYPE_FAILURE, exception.getCode());
        Assertions.assertFalse(exception.getMessage().contains(value));
        Assertions.assertTrue(exception.getDetails().isEmpty());
    }
}
