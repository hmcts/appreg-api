package uk.gov.hmcts.appregister.common.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.template.type.DateType;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

public class WordingTemplateTest {
    private static final String DATE_TEMPLATE =
            "This is a test {DATE|Applicant officer|10} with a date";

    private static final String TEXT_TEMPLATE2 =
            "This is a test {TEXT|Applicant officer|10} with a date";

    @Test
    void testTemplateFailParsingDataTypeIncorrect() {
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                WordingTemplateSentence.WordingTemplate.with(
                                        "{Wrong|Applicant officer|10}"));
        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE, appRegistryException.getCode());
    }

    @Test
    void testTemplateFailParsingTemplateFormatIncorrect() {
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> WordingTemplateSentence.WordingTemplate.with("{Wrong}"));
        Assertions.assertEquals(
                CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, appRegistryException.getCode());
    }

    @Test
    void testTemplateFailParsingTemplateFormatIncorrect2() {
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> WordingTemplateSentence.WordingTemplate.with("{Wrong"));
        Assertions.assertEquals(
                CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, appRegistryException.getCode());
    }

    @Test
    void testTemplateParsingAndSubstitution() {
        WordingTemplateSentence.WordingTemplate wordingTemplate =
                WordingTemplateSentence.WordingTemplate.with(DATE_TEMPLATE);

        Assertions.assertEquals("Applicant officer", wordingTemplate.getReference());
        Assertions.assertEquals(10, wordingTemplate.getLength());
        Assertions.assertEquals(DateType.class, wordingTemplate.getType().getClass());

        Assertions.assertTrue(wordingTemplate.doesSubstitute("2024-12-31"));
        Assertions.assertEquals("2024-12-31", wordingTemplate.substitute("2024-12-31"));
    }

    @Test
    public void testInvalidDateFormatFailure() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(DATE_TEMPLATE);
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                collection.substituteForTemplate(
                                        collection.getTemplateableContents()[0], "not date"));
        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE, appRegistryException.getCode());
    }

    @Test
    public void testInvalidLengthFormatFailure() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(TEXT_TEMPLATE2);
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                collection.substituteForTemplate(
                                        collection.getTemplateableContents()[0],
                                        "this value exceeds length"));
        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE, appRegistryException.getCode());
    }
}
