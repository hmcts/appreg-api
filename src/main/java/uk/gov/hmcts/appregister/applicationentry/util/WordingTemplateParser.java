package uk.gov.hmcts.appregister.applicationentry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class WordingTemplateParser {

    // TODO: Triple check this assumption.
    /* Matches wording tokens in the format {TYPE|NAME|MAX_LENGTH}
     * Example: {TEXT|Date|10}
     * - Group 1: TYPE (e.g. TEXT)
     * - Group 2: NAME (e.g. Date)
     * - Group 3: MAX LENGTH (e.g. 10)
     */
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\{([^|}]+)\\|([^|}]+)\\|(\\d+)}");
    private static final int TYPE = 1;
    private static final int NAME = 2;
    private static final int MAX_LENGTH = 3;

    public String generateWording(String template, List<String> inputFields) {
        List<TemplateToken> tokens = extractTokens(template);
        if (!hasCorrectNumberOfTexts(tokens, inputFields)) {
            throw new IllegalArgumentException(
                    "Expected "
                            + tokens.size()
                            + " text fields but received "
                            + (inputFields == null ? 0 : inputFields.size()));
        }

        if (!areTextsWithinLength(tokens, inputFields)) {
            throw new IllegalArgumentException(
                    "One or more text fields exceed the maximum allowed length.");
        }

        return injectInputFieldsIntoTemplate(template, inputFields);
    }

    private List<TemplateToken> extractTokens(String template) {
        List<TemplateToken> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        while (matcher.find()) {
            tokens.add(
                    new TemplateToken(
                            matcher.group(TYPE),
                            matcher.group(NAME),
                            Integer.parseInt(matcher.group(MAX_LENGTH))));
        }
        return tokens;
    }

    private boolean hasCorrectNumberOfTexts(List<TemplateToken> tokens, List<String> inputFields) {
        return inputFields != null && inputFields.size() >= tokens.size();
    }

    private boolean areTextsWithinLength(List<TemplateToken> tokens, List<String> inputFields) {
        for (int i = 0; i < tokens.size(); i++) {
            String value = inputFields.get(i);
            TemplateToken token = tokens.get(i);
            if (value.length() > token.maxLength()) {
                return false;
            }
        }
        return true;
    }

    private String injectInputFieldsIntoTemplate(String template, List<String> inputFields) {
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (matcher.find()) {
            String replacement = (i < inputFields.size()) ? inputFields.get(i) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            i++;
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
