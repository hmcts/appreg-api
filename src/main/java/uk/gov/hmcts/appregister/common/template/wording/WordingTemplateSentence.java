package uk.gov.hmcts.appregister.common.template.wording;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.ToString;
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
@ToString
public class WordingTemplateSentence implements TemplateableSentence {
    private List<WordingTemplate> contents = new ArrayList<>();

    /** The starting character. */
    private static final String START_CHARACTER = "{";

    /** The end character. */
    private static final String END_CHARACTER = "}";

    /** The original template sentence string. */
    private String template;

    /** The sanitised template string that is suitable for others to consume. */
    private String sanitisedTemplate;

    /** The erroneous templates that have been identified. */
    private List<String> erroneous = new ArrayList<>();

    /** The sentence template string with placeholders. This forms the substituted sentence */
    private String templateWithProcessedPlaceholders = "";

    /** The decomposed template details. */
    private TemplateDetail templateDetail;

    private static final String PARSING_LOG_MESSAGE = "Parsing wording template: {}";

    /** The regular expression to identify the template regex. */
    private static final String TEMPLATE_REGEX = "\\" + START_CHARACTER + "(.*?)\\" + END_CHARACTER;

    public WordingTemplateSentence(String templateString) {
        this.template = templateString;
        sanitisedTemplate = template;

        templateDetail = new TemplateDetail();

        templateWithProcessedPlaceholders = template;
        Pattern p = Pattern.compile(TEMPLATE_REGEX, Pattern.DOTALL);
        Matcher m = p.matcher(templateString);

        log.debug(PARSING_LOG_MESSAGE, templateString);

        int positionIndex = 0;
        while (m.find()) {
            String grp = m.group(1);
            log.debug(PARSING_LOG_MESSAGE, grp);

            try {
                WordingTemplate wordingTemplate = new WordingTemplate(grp);

                log.debug("Parsed wording template: {}", wordingTemplate.getDetail().getKey());
                contents.add(wordingTemplate);

                // create a sanitised template with positional placeholders that can be returned
                // from the API
                sanitisedTemplate =
                        sanitisedTemplate.replaceFirst(
                                "\\" + START_CHARACTER + Pattern.quote(grp) + "\\" + END_CHARACTER,
                                START_CHARACTER
                                        + START_CHARACTER
                                        + wordingTemplate.getDetail().getKey()
                                        + END_CHARACTER
                                        + END_CHARACTER);

                // replace the pattern with a placeholder
                templateWithProcessedPlaceholders =
                        templateWithProcessedPlaceholders.replaceFirst(
                                Pattern.quote(grp), wordingTemplate.getDetail().getKey());
                positionIndex = positionIndex + 1;
            } catch (AppRegistryException ex) {
                log.warn("Failing to parse template %s".formatted(grp), ex);

                // store the erroneous template for reporting
                erroneous.add(grp);
            }
        }

        templateDetail.setTemplate(sanitisedTemplate);

        log.debug(
                "Created template with positional placeholders: {}",
                templateWithProcessedPlaceholders);
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

        if ((values == null || values.isEmpty()) && contents.isEmpty()) {
            log.debug("No substitution values provided, returning original template");
            return BraceSubstitutedSentence.withSubstitutedSentence(returnedString);
        }

        if (values.size() > getTemplatesToBeProcessed()
                || values.size() < getTemplatesToBeProcessed()) {
            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Number of values exceeds number of templates",
                    Map.of(
                            "templateSize", Integer.toString(getTemplatesToBeProcessed()),
                            "valueSize", Integer.toString(values.size())));
        }

        // check the reference keys are valid according to the template details
        checkReferenceKeysAreValid(values);

        for (int i = 0; i < values.size(); i++) {
            WordingTemplate templateableForKey = getTemplateForKey(values.get(i).getKey());

            if (!contents.get(i).isSubstitutionComplete()
                    && values.get(i).getKey().equals(contents.get(i).getDetail().getKey())) {
                // if we have a value to substitute then substitute it into one of the valid
                // templates
                log.debug("Substituting options into template: {}", templateableForKey.toString());

                String subs = templateableForKey.substitute(values.get(i).getValue());

                // replace the template placeholder with the template value
                returnedString = returnedString.replace(contents.get(i).getDetail().getKey(), subs);
            }
        }

        templateWithProcessedPlaceholders = returnedString;

        log.debug("Substituted value: {}", returnedString);
        return BraceSubstitutedSentence.withSubstitutedSentence(returnedString);
    }

    /**
     * gets the template for the key specified.
     *
     * @return The template for the key
     */
    private WordingTemplate getTemplateForKey(String key) {
        for (WordingTemplate templateable : contents) {
            if (!templateable.isSubstitutionComplete()
                    && templateable.getDetail().getKey().equals(key)) {
                return templateable;
            }
        }
        throw new AppRegistryException(
                CommonAppError.WORDING_SUBSTITUTE_KEY_NOT_FOUND,
                "Reference key not found in template collection");
    }

    /**
     * gets the templates that have not been processed.
     *
     * @return The number of templates that have yet to be processed
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
    private void checkReferenceKeysAreValid(List<TemplateSubstitution> values) {
        for (TemplateSubstitution substitution : values) {
            if (getTemplateableForKey(substitution.getKey()) == null) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                        "Number of values exceeds number of templates. Invalid reference key: "
                                + substitution.getKey());
            }
        }
    }

    /**
     * gets a templateable entry for the key specified.
     *
     * @param key The key to find
     * @return The templateable entry or null if not found
     */
    private Templateable getTemplateableForKey(String key) {
        for (Templateable templatable : contents) {
            if (templatable.getDetail().getKey().equals(key)) {
                return templatable;
            }
        }
        return null;
    }

    @Override
    public List<TemplateSubstitution> getKeysToBeSubstituted() {
        List<TemplateSubstitution> substitutionList = new ArrayList<>();
        for (TemplateKeyWithConstraint constraint :
                templateDetail.getSubstitutionKeyConstraints()) {
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

        // find the template to substitute
        for (int i = 0; i < contents.size(); i++) {

            // find the matching template reference
            if (!contents.get(i).isSubstitutionComplete() && contents.get(i).equals(values)) {
                String sub = contents.get(i).substitute(value);

                log.debug("Substituted value into template: {}", contents.get(i).toString());

                // replace the template placeholder with the template value
                returnedString = returnedString.replace(contents.get(i).getDetail().getKey(), sub);
                templateWithProcessedPlaceholders = returnedString;

                log.debug("Substituted value into the sentence: {}", contents.get(i).toString());

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

    /**
     * A wording template that supports substitution. The wording template is of the form
     * {TYPE|REFERENCE|LENGTH}
     */
    @ToString
    @Getter
    public static class WordingTemplate implements Templateable {
        /** Then delimiter. */
        private static String DELIMITER = "\\|";

        private boolean substitutionComplete = false;

        /** The regex pattern to split the template. */
        private static final Pattern PATTERN =
                Pattern.compile(
                        "[^|]+\\" + DELIMITER + "[^|]+\\" + DELIMITER + "[^}]+", Pattern.DOTALL);

        private String value;

        /** The template string. */
        private TemplateKeyWithConstraint templateKeyWithConstraint;

        @Override
        public TemplateKeyWithConstraint getDetail() {
            templateKeyWithConstraint.setValue(value);
            return templateKeyWithConstraint;
        }

        private WordingTemplate(String templateString) {
            // check the template string is valid
            if (!PATTERN.matcher(templateString).find()) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            templateKeyWithConstraint = new TemplateKeyWithConstraint();

            String[] parts = getPartsOfTemplate(templateString);

            // if the parts in the template do not equate to 3 then throw an exception
            if (parts.length != 3) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            templateKeyWithConstraint = new TemplateKeyWithConstraint();
            TemplateConstraint constraint = new TemplateConstraint();
            templateKeyWithConstraint.setConstraint(constraint);

            // split the template stringand store the meta data parts
            String reference = parts[1];
            Integer length = Integer.parseInt(parts[2]);

            templateKeyWithConstraint.setKey(reference);
            constraint.setLength(length);

            // validates the data type
            DataType type = validateDataType(parts[0]);

            if (type == null) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_DATA_TYPE_FAILURE, "Invalid data type in template");
            }

            // validates the data type
            constraint.setType(TemplateConstraint.TypeEnum.valueOf(parts[0]));
        }

        /**
         * Creates a working template from a string.
         *
         * @param template The template string
         * @return wording template instance
         */
        public static WordingTemplate with(String template) {
            Pattern p = Pattern.compile(TEMPLATE_REGEX, Pattern.DOTALL);
            Matcher m = p.matcher(template);

            log.debug(PARSING_LOG_MESSAGE, template);

            boolean found = m.find();
            if (!found) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            String grp = m.group(1);
            log.debug(PARSING_LOG_MESSAGE, grp);

            return new WordingTemplate(grp);
        }

        /**
         * splits the pattern into parts.
         *
         * @param template The template to process
         * @return The pattern parts
         */
        private String[] getPartsOfTemplate(String template) {
            return template.split(DELIMITER);
        }

        @Override
        public void canValueBeSubstituted(String value) {
            DataType type = validateDataType(this.getDetail().getConstraint().getType().getValue());
            log.debug("Validating value '{}' for template: {}", value, this);
            if (!type.validateForType(value)) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_DATA_TYPE_FAILURE,
                        "Invalid data type value in template",
                        Map.of(this.getDetail().getKey(), value));
            }

            log.debug("Validating value length '{}' for template: {}", value.length(), this);
            if (value.length() > this.getDetail().getConstraint().getLength()) {
                throw new AppRegistryException(
                        CommonAppError.WORDING_LENGTH_FAILURE,
                        "Invalid length type in template",
                        Map.of(this.getDetail().getKey(), value));
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
         * @return The data type or null if not found
         */
        public static DataType validateDataType(String type) {
            // check the data types is correct in the template
            for (WordingDataTypes types : WordingDataTypes.values()) {
                if (types.getValue().equals(type)) {
                    return types.getType();
                }
            }

            return null;
        }

        @Override
        public boolean isSubstitutionComplete() {
            return substitutionComplete;
        }
    }
}
