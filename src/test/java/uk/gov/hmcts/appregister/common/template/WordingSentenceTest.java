package uk.gov.hmcts.appregister.common.template;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

public class WordingSentenceTest {
    private static final String MULTIPLE_VALUE_TEMPLATE =
            "Application by {TEXT|Applicant officer|10} for a production ord covering "
                    + "{DATE|No.of accounts|10} accounts(s) requiring the respondent to either produce or "
                    + "allow access to material that is in their possession or control for the purpose of "
                    + "a relevant investigation";

    private static final String MULTIPLE_INVALID =
            "Application by {TEXT|Applicant officer|70} for a production ord covering "
                    + "{NoType|No.of accounts|3} accounts(s) requiring the respondent to either produce or "
                    + "allow access to material that is in their possession or control for "
                    + "the purpose of {IncorrectFormat|} "
                    + "a relevant investigation {This is not a valid template} ";

    private static final String SINGLE_VALUE_TEMPLATE =
            "This is a test {DATE|Applicant officer|70} with a date";

    @Test
    public void testParseWordingTemplateMultipleSuccess() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, collection.getTemplateableContents().length);

        Assertions.assertEquals(
                "Application by {{Applicant officer}} for a "
                        + "production ord covering {{No.of accounts}} accounts(s) requiring the respondent to "
                        + "either produce or allow access to material that is in their possession or control for the "
                        + "purpose of a relevant investigation",
                collection.getDetail().getTemplate());
        Assertions.assertEquals(
                "Applicant officer",
                collection.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                collection.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution2.setKey("No.of accounts");
        substitution2.setValue("2025-03-17");

        java.lang.String result = collection.substitute(List.of(substitution, substitution2));
        Assertions.assertEquals(
                "Application by My Test for a production ord covering 2025-03-17 accounts(s) "
                        + "requiring the respondent to either produce or allow access to material that is in their "
                        + "possession or control for the purpose of a relevant investigation",
                result);
        Assertions.assertTrue(collection.getErroneousTemplates().isEmpty());
    }

    @Test
    public void testParseWordingTemplateSingleSuccess() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(SINGLE_VALUE_TEMPLATE);

        Assertions.assertEquals(1, collection.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                collection
                        .getTemplateForReference(
                                collection
                                        .getDetail()
                                        .getSubstitutionKeyConstraints()
                                        .get(0)
                                        .getKey())
                        .getDetail()
                        .getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("2025-03-17");

        java.lang.String result = collection.substitute(List.of(substitution));
        Assertions.assertEquals("This is a test 2025-03-17 with a date", result);
        Assertions.assertTrue(collection.getErroneousTemplates().isEmpty());
        Assertions.assertEquals(
                "This is a test 2025-03-17 with a date", collection.getSubstitutedSentence());
    }

    @Test
    public void testParseWordingTemplateMultipleSuccessForEachTemplateSeparately() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(2, collection.getTemplateableContents().length);
        TemplateableSentence sentence =
                collection.substituteForTemplate(
                        collection.getTemplateableContents()[1], "2025-03-17");
        Assertions.assertEquals(1, sentence.getTemplateableContents().length);
        sentence = sentence.substituteForTemplate(sentence.getTemplateableContents()[0], "Test");
        Assertions.assertEquals(
                "Application by Test for a production ord covering 2025-03-17 accounts(s) "
                        + "requiring the respondent to either produce or allow access to material that is in their "
                        + "possession or control for the purpose of a relevant investigation",
                sentence.getSubstitutedSentence());
    }

    @Test
    public void testGetReferences() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(2, collection.getDetail().getSubstitutionKeyConstraints().size());
        Assertions.assertEquals(
                "Applicant officer",
                collection.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                collection.getDetail().getSubstitutionKeyConstraints().get(1).getKey());
    }

    @Test
    public void testGetTemplate() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(
                "Application by {{Applicant officer}} for a production "
                        + "ord covering {{No.of accounts}} accounts(s) "
                        + "requiring the respondent to either produce or "
                        + "allow access to material that is in their possession "
                        + "or control for the purpose of a relevant investigation",
                collection.getDetail().getTemplate());
    }

    @Test
    public void testParseWordingParsingInvalidTemplates() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_INVALID);

        Assertions.assertEquals(1, collection.getTemplateableContents().length);
        Assertions.assertEquals(3, collection.getErroneousTemplates().size());
        Assertions.assertEquals(
                "NoType|No.of accounts|3", collection.getErroneousTemplates().get(0));
        Assertions.assertEquals("IncorrectFormat|", collection.getErroneousTemplates().get(1));
        Assertions.assertEquals(
                "This is not a valid template", collection.getErroneousTemplates().get(2));
    }

    @Test
    public void testInvalidDateFormatFailure() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                collection.substituteForTemplate(
                                        collection.getTemplateableContents()[1], "not date"));
        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE, appRegistryException.getCode());
    }

    @Test
    public void testInvalidLengthFormatFailure() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
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

    @Test
    public void testInvalidNumberOfArgumentsTooMany() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, collection.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                collection.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                collection.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution2.setKey("No.of accounts");
        substitution2.setValue("2025-03-17");

        TemplateSubstitution substitution3 = new TemplateSubstitution();
        substitution3.setKey("invalid");
        substitution3.setValue("2025-03-17");

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                collection.substitute(
                                        List.of(substitution, substitution2, substitution3)));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    @Test
    public void testInvalidNumberOfArgumentsTooFew() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, collection.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                collection.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                collection.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> collection.substitute(List.of(substitution)));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    @Test
    public void testInvalidArgumentSubstitutionKey() {
        WordingTemplateSentence collection = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, collection.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                collection.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                collection.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution.setKey("No.of accounts wrong");
        substitution.setValue("My Test");

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> collection.substitute(List.of(substitution, substitution2)));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }
}
