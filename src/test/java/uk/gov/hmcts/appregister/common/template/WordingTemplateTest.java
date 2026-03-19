package uk.gov.hmcts.appregister.common.template;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
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
    public void testParameterSizeMismatch() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(DATE_TEMPLATE);
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> collection.substitute(List.of()));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    // TODO: Re-enable this once the decision has been made on the FE implementation.
    /*
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
    } */

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
