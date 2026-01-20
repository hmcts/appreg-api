package uk.gov.hmcts.appregister.common.template;

import java.util.ArrayList;
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
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);

        Assertions.assertEquals(
                "Application by {{Applicant officer}} for a "
                        + "production ord covering {{No.of accounts}} accounts(s) requiring the respondent to "
                        + "either produce or allow access to material that is in their possession or control for the "
                        + "purpose of a relevant investigation",
                templateSentence.getDetail().getTemplate());
        Assertions.assertEquals(
                "Applicant officer",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getKey());

        Assertions.assertEquals(
                "No.of accounts",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution2.setKey("No.of accounts");
        substitution2.setValue("2025-03-17");

        SubstitutedSentence result =
                templateSentence.substitute(List.of(substitution, substitution2));
        Assertions.assertEquals(
                "Applicant officer",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "My Test",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getValue());

        Assertions.assertEquals(
                "No.of accounts",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getKey());
        Assertions.assertEquals(
                "2025-03-17",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getValue());

        Assertions.assertEquals(
                "Application by {My Test} for a production ord covering {2025-03-17} accounts(s) "
                        + "requiring the respondent to either produce or allow access to material that is in their "
                        + "possession or control for the purpose of a relevant investigation",
                result.getSubstitutedString());
        Assertions.assertTrue(templateSentence.getErroneousTemplates().isEmpty());

        // verify getting values for the substituted string
        List<String> templateSubstitutionList =
                templateSentence.getSubstitutedSentence().getAppliedValues();
        Assertions.assertEquals("My Test", templateSubstitutionList.get(0));
        Assertions.assertEquals("2025-03-17", templateSubstitutionList.get(1));
    }

    @Test
    public void testGetSubstitutedSentence() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);

        Assertions.assertEquals(
                "Application by {{Applicant officer}} for a "
                        + "production ord covering {{No.of accounts}} accounts(s) requiring the respondent to "
                        + "either produce or allow access to material that is in their possession or control for the "
                        + "purpose of a relevant investigation",
                templateSentence.getDetail().getTemplate());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution2.setKey("No.of accounts");
        substitution2.setValue("2025-03-17");

        SubstitutedSentence result =
                templateSentence.substitute(List.of(substitution, substitution2));
        Assertions.assertEquals(
                "Application by {My Test} for a production "
                        + "ord covering {2025-03-17} accounts(s) requiring "
                        + "the respondent to either produce or allow access "
                        + "to material that is in their possession or control "
                        + "for the purpose of a relevant investigation",
                result.getSubstitutedString());

        List<TemplateSubstitution> templateSubstitution = templateSentence.getKeysToBeSubstituted();

        // apply sentence values to the template substitution keys
        result.applyValuesTo(templateSubstitution);
        Assertions.assertEquals("Applicant officer", templateSubstitution.get(0).getKey());
        Assertions.assertEquals("My Test", templateSubstitution.get(0).getValue());
        Assertions.assertEquals("No.of accounts", templateSubstitution.get(1).getKey());
        Assertions.assertEquals("2025-03-17", templateSubstitution.get(1).getValue());

        // substitute again to verify the extracted value data
        templateSentence = WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        result = templateSentence.substitute(templateSubstitution);
        Assertions.assertEquals(
                "Application by {My Test} for a production "
                        + "ord covering {2025-03-17} accounts(s) requiring "
                        + "the respondent to either produce or allow access "
                        + "to material that is in their possession or control "
                        + "for the purpose of a relevant investigation",
                result.getSubstitutedString());
    }

    @Test
    public void testGetSubstitutedSentenceApplyValues() {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("No.of accounts");

        List<TemplateSubstitution> templateSubstitutionList = new ArrayList<>();
        templateSubstitutionList.add(substitution);
        templateSubstitutionList.add(substitution1);

        BraceSubstitutedSentence substitutedSentence =
                BraceSubstitutedSentence.withSubstitutedSentence(
                        "Application by {My Test} for a production "
                                + "ord covering {2025-03-17} accounts(s)");

        // pass an empty list of keys for the values to be applied to
        Assertions.assertTrue(substitutedSentence.applyValuesTo(templateSubstitutionList));

        // ensure that the list is populated but unchanged
        Assertions.assertEquals("Applicant officer", templateSubstitutionList.get(0).getKey());
        Assertions.assertEquals("My Test", templateSubstitutionList.get(0).getValue());
        Assertions.assertEquals("No.of accounts", templateSubstitutionList.get(1).getKey());
        Assertions.assertEquals("2025-03-17", templateSubstitutionList.get(1).getValue());
    }

    @Test
    public void testGetSubstitutedSentenceError() {
        BraceSubstitutedSentence substitutedSentence =
                BraceSubstitutedSentence.withSubstitutedSentence(
                        "Application by {My Test} for a production "
                                + "ord covering {NotADate} accounts(s)");

        List<TemplateSubstitution> templateSubstitutionList = List.of();

        // pass an empty list of keys for the values to be applied to
        Assertions.assertFalse(substitutedSentence.applyValuesTo(templateSubstitutionList));

        // ensure that the list remains untouched
        Assertions.assertEquals(0, templateSubstitutionList.size());
    }

    @Test
    public void testSubstituteIntoWordingTemplateSingleSuccess() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(SINGLE_VALUE_TEMPLATE);

        Assertions.assertEquals(1, templateSentence.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                templateSentence
                        .getTemplateForReference(
                                templateSentence
                                        .getDetail()
                                        .getSubstitutionKeyConstraints()
                                        .get(0)
                                        .getKey())
                        .getDetail()
                        .getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("2025-03-17");

        SubstitutedSentence result = templateSentence.substitute(List.of(substitution));
        Assertions.assertEquals(
                "This is a test {2025-03-17} with a date", result.getSubstitutedString());
        Assertions.assertTrue(templateSentence.getErroneousTemplates().isEmpty());
        Assertions.assertEquals(
                "This is a test {2025-03-17} with a date",
                templateSentence.getSubstitutedSentence().getSubstitutedString());
    }

    @Test
    public void testSubstituteIntoWordingTemplateAlreadyProcessed() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(SINGLE_VALUE_TEMPLATE);

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("2025-03-17");

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            templateSentence.substitute(List.of(substitution));
                            templateSentence.substitute(List.of(substitution));
                        });

        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, exception.getCode());
    }

    @Test
    public void testParseWordingTemplateMultipleSuccessForEachTemplateSeparately() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);
        TemplateableSentence sentence =
                templateSentence.substituteForTemplate(
                        templateSentence.getTemplateableContents()[1], "2025-03-17");
        Assertions.assertEquals(1, sentence.getTemplateableContents().length);
        sentence = sentence.substituteForTemplate(sentence.getTemplateableContents()[0], "Test");
        Assertions.assertEquals(
                "Application by {Test} for a production ord covering {2025-03-17} accounts(s) "
                        + "requiring the respondent to either produce or allow access to material that is in their "
                        + "possession or control for the purpose of a relevant investigation",
                sentence.getSubstitutedSentence().getSubstitutedString());
    }

    @Test
    public void testSubstituteTemplateKeyAlreadyProcessed() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);
        final TemplateableSentence sentence =
                templateSentence.substituteForTemplate(
                        templateSentence.getTemplateableContents()[1], "2025-03-17");
        Assertions.assertEquals(1, sentence.getTemplateableContents().length);

        Templateable templateableToSubstitute = sentence.getTemplateableContents()[0];

        // perform substitution twice so we force an error
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> {
                            sentence.substituteForTemplate(templateableToSubstitute, "Test");
                            sentence.substituteForTemplate(templateableToSubstitute, "Test");
                        });

        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_KEY_NOT_FOUND, appRegistryException.getCode());
    }

    @Test
    public void testGetReferences() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(
                2, templateSentence.getDetail().getSubstitutionKeyConstraints().size());
        Assertions.assertEquals(
                "Applicant officer",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getKey());
    }

    @Test
    public void testGetTemplate() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        Assertions.assertEquals(
                "Application by {{Applicant officer}} for a production "
                        + "ord covering {{No.of accounts}} accounts(s) "
                        + "requiring the respondent to either produce or "
                        + "allow access to material that is in their possession "
                        + "or control for the purpose of a relevant investigation",
                templateSentence.getDetail().getTemplate());
    }

    @Test
    public void testParseWordingParsingInvalidTemplates() {
        WordingTemplateSentence templateSentence = WordingTemplateSentence.with(MULTIPLE_INVALID);

        Assertions.assertEquals(1, templateSentence.getTemplateableContents().length);
        Assertions.assertEquals(3, templateSentence.getErroneousTemplates().size());
        Assertions.assertEquals(
                "NoType|No.of accounts|3", templateSentence.getErroneousTemplates().get(0));
        Assertions.assertEquals(
                "IncorrectFormat|", templateSentence.getErroneousTemplates().get(1));
        Assertions.assertEquals(
                "This is not a valid template", templateSentence.getErroneousTemplates().get(2));
    }

    @Test
    public void testInvalidDateFormatFailure() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                templateSentence.substituteForTemplate(
                                        templateSentence.getTemplateableContents()[1], "not date"));
        Assertions.assertEquals(
                CommonAppError.WORDING_DATA_TYPE_FAILURE, appRegistryException.getCode());
    }

    @Test
    public void testInvalidLengthFormatFailure() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                templateSentence.substituteForTemplate(
                                        templateSentence.getTemplateableContents()[0],
                                        "this value exceeds length"));
        Assertions.assertEquals(
                CommonAppError.WORDING_LENGTH_FAILURE, appRegistryException.getCode());
    }

    @Test
    public void testInvalidNumberOfArgumentsTooMany() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

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
                                templateSentence.substitute(
                                        List.of(substitution, substitution2, substitution3)));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    @Test
    public void testInvalidNumberOfArgumentsTooFew() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> templateSentence.substitute(List.of(substitution)));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }

    @Test
    public void testInvalidArgumentSubstitutionKey() {
        WordingTemplateSentence templateSentence =
                WordingTemplateSentence.with(MULTIPLE_VALUE_TEMPLATE);

        Assertions.assertEquals(2, templateSentence.getTemplateableContents().length);

        Assertions.assertEquals(
                "Applicant officer",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                "No.of accounts",
                templateSentence.getDetail().getSubstitutionKeyConstraints().get(1).getKey());

        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Applicant officer");
        substitution.setValue("My Test");

        TemplateSubstitution substitution2 = new TemplateSubstitution();
        substitution.setKey("No.of accounts wrong");
        substitution.setValue("My Test");

        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> templateSentence.substitute(List.of(substitution, substitution2)));
        Assertions.assertEquals(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH, appRegistryException.getCode());
    }
}
