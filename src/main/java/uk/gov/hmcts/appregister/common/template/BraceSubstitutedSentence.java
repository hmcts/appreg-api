package uk.gov.hmcts.appregister.common.template;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * A substituted brace sentence implementation. This class delineates values based on the use of
 * braces { } to identify substituted values.
 */
@Slf4j
public class BraceSubstitutedSentence implements SubstitutedSentence {
    /** The starting character. */
    private static final String START_CHARACTER = "{";

    /** The end character. */
    private static final String END_CHARACTER = "}";

    /** The value store for the sentence. */
    private final List<String> substitutedSentenceValues = new ArrayList<>();

    /** The regular expression to identify the template regex. */
    private static final String TEMPLATE_REGEX = "\\" + START_CHARACTER + "(.*?)\\" + END_CHARACTER;

    private String substitutedSentenceContent;

    private BraceSubstitutedSentence() {
        // hide constructor
    }

    /**
     * Parses the string and holds the values.
     *
     * @param substitutedSentence The substituted sentence string
     * @return The brace substituted sentence
     */
    public static BraceSubstitutedSentence withSubstitutedSentence(String substitutedSentence) {
        BraceSubstitutedSentence sentence = new BraceSubstitutedSentence();
        sentence.parse(substitutedSentence);
        sentence.substitutedSentenceContent = substitutedSentence;
        return sentence;
    }

    @Override
    public List<String> getAppliedValues() {
        return substitutedSentenceValues;
    }

    @Override
    public boolean applyValuesTo(List<TemplateSubstitution> values) {
        // apply values to the substituted keys in positional order. This code
        // copes with the situation where the amount of values provided does not
        // match the amount of keys that were substituted in the sentence by
        // not applying any values at all.
        if (values.size() != substitutedSentenceValues.size()) {
            log.warn(
                    "The amount of values provided ({}) does not match the "
                            + "amount of substituted keys ({}).Defaulting to"
                            + "no values being provided",
                    values.size(),
                    substitutedSentenceValues.size());
            return false;
        }

        // apply the values to the substitution key
        for (int i = 0; i < values.size(); i++) {
            values.get(i).setValue(substitutedSentenceValues.get(i));
        }

        return true;
    }

    @Override
    public String getSubstitutedString() {
        return substitutedSentenceContent;
    }

    /**
     * parses the list and stores the values.
     *
     * @param substitutedTemplateContent The substitution string
     */
    private void parse(String substitutedTemplateContent) {
        Pattern p = Pattern.compile(TEMPLATE_REGEX, Pattern.DOTALL);
        Matcher m = p.matcher(substitutedTemplateContent);

        int positionIndex = 0;
        while (m.find()) {
            substitutedSentenceValues.add(m.group(1));
            positionIndex = positionIndex + 1;
        }
    }
}
