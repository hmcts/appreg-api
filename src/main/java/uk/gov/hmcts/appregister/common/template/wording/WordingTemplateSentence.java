package uk.gov.hmcts.appregister.common.template.wording;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.template.BraceSubstitutedSentence;
import uk.gov.hmcts.appregister.common.template.SubstitutedSentence;
import uk.gov.hmcts.appregister.common.template.Templateable;
import uk.gov.hmcts.appregister.common.template.TemplateableSentence;
import uk.gov.hmcts.appregister.common.template.type.DataType;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * A class that allows us to parse multiple Wording Templates as part of a sentence e.g. This is one
 * value {TYPE|REFERENCE|LENGTH} and this is another {TYPE|REFERENCE|LENGTH}.
 *
 * <p>Each template needs to have the following string format:=
 *
 * <p>TYPE - The data type (e.g. TEXT) REFERENCE - The reference name for the data LENGTH - The
 * length of the data E.g. {TEXT|Applicant Name|50}
 */
@Slf4j
public class WordingTemplateSentence implements TemplateableSentence {
    private final List<WordingTemplate> contents = new ArrayList<>();
    private final List<String> placeholderTokens = new ArrayList<>();

    /** The starting character. */
    private static final String START_CHARACTER = "{";

    /** The end character. */
    private static final String END_CHARACTER = "}";

    /** The original template sentence string. */
    private final String template;

    /** The sanitised template string that is suitable for others to consume. */
    private String sanitisedTemplate;

    /** The erroneous templates that have been identified. */
    private final List<String> erroneous = new ArrayList<>();

    /** The sentence template string with placeholders. This forms the substituted sentence */
    private String templateWithProcessedPlaceholders = "";

    /** The decomposed template details. */
    private TemplateDetail templateDetail;

    /** The regular expression to identify the template regex. */
    private static final String TEMPLATE_REGEX = "\\" + START_CHARACTER + "(.*?)\\" + END_CHARACTER;

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(TEMPLATE_REGEX, Pattern.DOTALL);

    public WordingTemplateSentence(String templateString) {
        this.template = templateString;
        templateDetail = new TemplateDetail();

        Matcher m = TEMPLATE_PATTERN.matcher(templateString);

        StringBuilder sanitisedBuilder = new StringBuilder();
        StringBuilder placeholderBuilder = new StringBuilder();
        int lastMatchEnd = 0;

        while (m.find()) {
            sanitisedBuilder.append(templateString, lastMatchEnd, m.start());
            placeholderBuilder.append(templateString, lastMatchEnd, m.start());

            String grp = m.group(1);

            try {
                WordingTemplate wordingTemplate = new WordingTemplate(grp);
                String placeholderToken = buildPlaceholderToken(placeholderTokens.size());

                contents.add(wordingTemplate);
                placeholderTokens.add(placeholderToken);

                sanitisedBuilder
                        .append(START_CHARACTER)
                        .append(START_CHARACTER)
                        .append(wordingTemplate.getDetail().getKey())
                        .append(END_CHARACTER)
                        .append(END_CHARACTER);
                placeholderBuilder
                        .append(START_CHARACTER)
                        .append(placeholderToken)
                        .append(END_CHARACTER);
            } catch (AppRegistryException ex) {
                log.warn("Failed to parse wording template placeholder: {}", ex.getCode());

                // store the erroneous template for reporting
                erroneous.add(grp);
                sanitisedBuilder.append(m.group(0));
                placeholderBuilder.append(m.group(0));
            }

            lastMatchEnd = m.end();
        }

        sanitisedBuilder.append(templateString, lastMatchEnd, templateString.length());
        placeholderBuilder.append(templateString, lastMatchEnd, templateString.length());

        sanitisedTemplate = sanitisedBuilder.toString();
        templateWithProcessedPlaceholders = placeholderBuilder.toString();
        templateDetail.setTemplate(sanitisedTemplate);

        log.debug("Parsed wording template with {} placeholders", contents.size());
    }

    @Override
    public TemplateDetail getDetail() {
        templateDetail.setSubstitutionKeyConstraints(new ArrayList<>());
        for (Templateable wordingTemplate : contents) {
            // add the template detail to the collection detail
            templateDetail.addSubstitutionKeyConstraintsItem(wordingTemplate.getDetail());
        }

        return templateDetail;
    }

    @Override
    public boolean isSubstitutionComplete() {
        return false;
    }

    @Override
    public Templateable[] getTemplateableContents() {
        return contents.stream()
                .filter(p -> !p.isSubstitutionComplete())
                .toArray(Templateable[]::new);
    }

    @Override
    public SubstitutedSentence substitute(List<TemplateSubstitution> values) {
        String returnedString = templateWithProcessedPlaceholders;

        if (values == null) {
            if (contents.isEmpty()) {
                // No templates AND no values, safe to return the original template
                log.debug("No substitution values provided, returning original template");
                return BraceSubstitutedSentence.withSubstitutedSentence(returnedString);
            }

            // use a linked map for the detail map to maintain the order
            LinkedHashMap<String, String> detailedMap = new LinkedHashMap<>();
            detailedMap.put("valueSize", "null");
            detailedMap.put("templateSize", Integer.toString(getTemplatesToBeProcessed()));

            // Templates exist but values are null, invalid scenario
            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Substitution values cannot be null when template contains placeholders",
                    detailedMap);
        }

        if ((values.isEmpty()) && contents.isEmpty()) {
            log.debug("No substitution values provided, returning original template");
            return BraceSubstitutedSentence.withSubstitutedSentence(returnedString);
        }

        int templatesToBeProcessed = getTemplatesToBeProcessed();
        if (values.size() != templatesToBeProcessed) {

            // use a linked map for the detail map to maintain the order
            LinkedHashMap<String, String> detailedMap = new LinkedHashMap<>();
            detailedMap.put("valueSize", Integer.toString(values.size()));
            detailedMap.put("templateSize", Integer.toString(templatesToBeProcessed));

            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Number of values exceeds number of templates",
                    detailedMap);
        }

        // check the reference keys are valid according to the template details
        validateReferenceKeysAreValid(values);
        Map<String, LinkedList<String>> substitutionsByKey = groupSubstitutionsByKey(values);

        for (int i = 0; i < contents.size(); i++) {
            WordingTemplate templateable = contents.get(i);
            if (!templateable.isSubstitutionComplete()) {
                String key = templateable.getDetail().getKey();
                String value = substitutionsByKey.get(key).removeFirst();

                String subs = templateable.substitute(value);

                returnedString = returnedString.replace(placeholderTokens.get(i), subs);
            }
        }

        templateWithProcessedPlaceholders = returnedString;

        log.debug("Substituted {} wording values", values.size());
        return BraceSubstitutedSentence.withSubstitutedSentence(returnedString);
    }

    /**
     * gets the template for the key specified.
     *
     * @return The template for the key
     */
    private int getTemplatesToBeProcessed() {
        int contentSize = 0;
        for (Templateable templateable : contents) {
            if (!templateable.isSubstitutionComplete()) {
                contentSize = contentSize + 1;
            }
        }
        return contentSize;
    }

    /**
     * check the reference key is valid.
     *
     * @param values The list of substitution values provided
     */
    private void validateReferenceKeysAreValid(List<TemplateSubstitution> values) {
        Map<String, Integer> remainingKeyCounts = new HashMap<>();
        for (WordingTemplate templateable : contents) {
            if (!templateable.isSubstitutionComplete()) {
                remainingKeyCounts.merge(templateable.getDetail().getKey(), 1, Integer::sum);
            }
        }

        Map<String, Integer> substitutionKeyCounts = new HashMap<>();
        for (TemplateSubstitution substitution : values) {
            substitutionKeyCounts.merge(substitution.getKey(), 1, Integer::sum);
        }

        if (!remainingKeyCounts.equals(substitutionKeyCounts)) {
            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Number of values exceeds number of templates. Invalid reference keys");
        }
    }

    private Map<String, LinkedList<String>> groupSubstitutionsByKey(
            List<TemplateSubstitution> values) {
        Map<String, LinkedList<String>> substitutionsByKey = new LinkedHashMap<>();
        for (TemplateSubstitution substitution : values) {
            substitutionsByKey
                    .computeIfAbsent(substitution.getKey(), ignored -> new LinkedList<>())
                    .addLast(substitution.getValue());
        }
        return substitutionsByKey;
    }

    @Override
    public List<TemplateSubstitution> getKeysToBeSubstituted() {
        List<TemplateSubstitution> substitutionList = new ArrayList<>();
        for (TemplateKeyWithConstraint constraint : getDetail().getSubstitutionKeyConstraints()) {
            TemplateSubstitution templateSubstitution = new TemplateSubstitution();
            templateSubstitution.setKey(constraint.getKey());
            substitutionList.add(templateSubstitution);
        }
        return substitutionList;
    }

    /**
     * Creates a working template from a string.
     *
     * @param template The template string
     * @return wording template instance
     */
    public static WordingTemplateSentence with(String template) {
        return new WordingTemplateSentence(template);
    }

    @Override
    public List<String> getErroneousTemplates() {
        return erroneous;
    }

    @Override
    public TemplateableSentence substituteForTemplate(Templateable values, String value) {
        String returnedString = templateWithProcessedPlaceholders;

        for (int i = 0; i < contents.size(); i++) {
            if (!contents.get(i).isSubstitutionComplete() && contents.get(i).equals(values)) {
                String sub = contents.get(i).substitute(value);

                returnedString = returnedString.replace(placeholderTokens.get(i), sub);
                templateWithProcessedPlaceholders = returnedString;

                log.debug("Substituted wording value at position {}", i);

                return this;
            }
        }

        throw new AppRegistryException(
                CommonAppError.WORDING_SUBSTITUTE_KEY_NOT_FOUND,
                "Reference key not found in template collection");
    }

    @Override
    public SubstitutedSentence getSubstitutedSentence() {
        return BraceSubstitutedSentence.withSubstitutedSentence(templateWithProcessedPlaceholders);
    }

    @Override
    public Templateable getTemplateForReference(String referenceValue) {
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i).getDetail().getKey().equals(referenceValue)
                    && !contents.get(i).isSubstitutionComplete()) {
                return contents.get(i);
            }
        }

        return null;
    }

    private static String buildPlaceholderToken(int index) {
        return "__WORDING_TEMPLATE_PLACEHOLDER_%d__".formatted(index);
    }

    /**
     * A wording template that supports substitution. The wording template is of the form
     * {TYPE|REFERENCE|LENGTH}
     */
    @Getter
    public static class WordingTemplate implements Templateable {
        /** The delimiter used within a wording template. */
        private static final String DELIMITER = "|";

        private boolean substitutionComplete;

        private String value;

        /** The template string. */
        private TemplateKeyWithConstraint templateKeyWithConstraint;

        @Override
        public TemplateKeyWithConstraint getDetail() {
            templateKeyWithConstraint.setValue(value);
            return templateKeyWithConstraint;
        }

        private WordingTemplate(String templateString) {
            String[] parts = getPartsOfTemplate(templateString);

            // templates must have exactly TYPE|REFERENCE|LENGTH, preserving blank segments.
            if (parts.length != 3
                    || parts[0].isBlank()
                    || parts[1].isBlank()
                    || parts[2].isBlank()) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            templateKeyWithConstraint = new TemplateKeyWithConstraint();
            TemplateConstraint constraint = new TemplateConstraint();
            templateKeyWithConstraint.setConstraint(constraint);

            // split the template stringand store the meta data parts
            String reference = parts[1];
            Integer length;
            try {
                length = Integer.parseInt(parts[2]);
            } catch (NumberFormatException exception) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE,
                        "Invalid template string",
                        exception);
            }

            templateKeyWithConstraint.setKey(reference);
            constraint.setLength(length);

            // validates the data type
            WordingDataTypes type = validateDataType(parts[0]);

            if (type == null) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_DATA_TYPE_FAILURE, "Invalid data type in template");
            }

            // validates the data type
            constraint.setType(type.getValue());
        }

        /**
         * Creates a working template from a string.
         *
         * @param template The template string
         * @return wording template instance
         */
        public static WordingTemplate with(String template) {
            Matcher m = TEMPLATE_PATTERN.matcher(template);

            boolean found = m.find();
            if (!found) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            String grp = m.group(1);
            return new WordingTemplate(grp);
        }

        /**
         * splits the pattern into parts.
         *
         * @param template The template to process
         * @return The pattern parts
         */
        private String[] getPartsOfTemplate(String template) {
            return template.split(Pattern.quote(DELIMITER), -1);
        }

        @Override
        public void canValueBeSubstituted(String value) {
            DataType type =
                    validateDataType(this.getDetail().getConstraint().getType().getValue())
                            .getType();
            if (!type.validateForType(value)) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_DATA_TYPE_FAILURE,
                        "Invalid data type value in template");
            }

            if (value.length() > this.getDetail().getConstraint().getLength()) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_LENGTH_FAILURE,
                        "Invalid length type in template: expected %d but got %d"
                                .formatted(
                                        this.getDetail().getConstraint().getLength(),
                                        value.length()));
            }
        }

        /**
         * substitute the value into the template.
         *
         * @param value The value to substitute
         * @return The substituted string or not present if validation failed. NOTE: This method
         *     simply returns the original string if substitution can be performed
         */
        private String substitute(String value) {
            canValueBeSubstituted(value);
            getDetail().setValue(value);
            this.value = value;
            substitutionComplete = true;
            return value;
        }

        /**
         * gets a java data type class for a template type.
         *
         * @return Always return a TEXT for now.
         */
        @SuppressWarnings({"java:S1172", "java:S1135"})
        public static WordingDataTypes validateDataType(String type) {
            // TODO: When we know more about how specific data types work
            // we can interpret and validate for them. At the moment lets return a
            // TEXT which is completely open and accepts any value.
            return WordingDataTypes.TEXT;
        }

        @Override
        public boolean isSubstitutionComplete() {
            return substitutionComplete;
        }
    }
}
