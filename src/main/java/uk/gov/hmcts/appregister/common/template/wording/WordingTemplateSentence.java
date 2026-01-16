package uk.gov.hmcts.appregister.common.template.wording;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.template.Templateable;
import uk.gov.hmcts.appregister.common.template.TemplateableSentence;
import uk.gov.hmcts.appregister.common.template.type.DataType;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * A class that allows us to parse a Wording Templates sentence containing multiple work templates
 * of the form {TYPE|REFERENCE|LENGTH} and substitute values into it.
 *
 * <p>Each template needs to have the following string format:=
 *
 * <p>TYPE - The data type (e.g. TEXT) REFERENCE - The reference name for the data LENGTH - The
 * length of the data E.g. {TEXT|Applicant Name|50}
 */
@Slf4j
@ToString
public class WordingTemplateSentence implements TemplateableSentence {
    private List<Templateable> contents = new ArrayList<>();

    /** The starting character. */
    private static final String START_CHARACTER = "{";

    /** The end character. */
    private static final String END_CHARACTER = "}";

    /** The original template string. */
    private String template;

    /** The original template string. */
    private String sanitisedTemplate;

    /** The erroneous templates that have been identified. */
    private List<String> erroneous = new ArrayList<>();

    /** The template string with placeholders. */
    private String templateWithProcessedPlaceholders = "";

    private TemplateDetail templateDetail;

    private static final String PARSING_LOG_MESSAGE = "Parsing wording template: {}";

    /**
     * The placeholder UUID that is used as a unique placeholder with the template. Without this we
     * can not guarantee a unique substitution key.
     */
    private UUID positionalPlaceholderPrefix = UUID.randomUUID();

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

                // add the template detail to the collection detail
                templateDetail.addSubstitutionKeyConstraintsItem(wordingTemplate.getDetail());

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
                                "\\" + START_CHARACTER + Pattern.quote(grp) + "\\" + END_CHARACTER,
                                START_CHARACTER
                                        + wordingTemplate.getDetail().getKey()
                                        + END_CHARACTER);
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

    /**
     * A constructor that copies an existing template collection but uses a different positional
     * template string.
     *
     * @param templateToCopy The template to copy
     * @param templateWithPlaceholders The processed placeholder string to work with
     * @param contents The contents remaining to be substituted
     */
    WordingTemplateSentence(
            WordingTemplateSentence templateToCopy,
            String templateWithPlaceholders,
            List<Templateable> contents) {
        this.templateWithProcessedPlaceholders = templateWithPlaceholders;
        this.erroneous = templateToCopy.erroneous;
        this.positionalPlaceholderPrefix = templateToCopy.positionalPlaceholderPrefix;
        this.template = templateToCopy.template;
        this.contents = contents;
        this.sanitisedTemplate = templateToCopy.sanitisedTemplate;
    }

    @Override
    public TemplateDetail getDetail() {
        return templateDetail;
    }

    @Override
    public Templateable[] getTemplateableContents() {
        return contents.toArray(new Templateable[0]);
    }

    @Override
    public String substitute(List<TemplateSubstitution> values) {
        String returnedString = templateWithProcessedPlaceholders;

        if ((values == null || values.isEmpty()) && contents.isEmpty()) {
            log.debug("No substitution values provided, returning original template");
            return returnedString;
        }

        if (values.size() > contents.size() || values.size() < contents.size()) {
            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Number of values exceeds number of templates",
                    Map.of(
                            "templateSize", Integer.toString(contents.size()),
                            "valueSize", Integer.toString(values.size())));
        }

        // check the reference keys are valid according to the template details
        checkReferenceKeysAreValid(values);

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).getKey().equals(contents.get(i).getDetail().getKey())) {
                // if we have a value to substitute then substitute it into one of the valid
                // templates
                log.debug("Substituting options into template: {}", contents.get(i).toString());

                String subs = contents.get(i).substitute(values.get(i).getValue());

                // replace the template placeholder with the template value
                returnedString =
                        returnedString.replace(
                                START_CHARACTER
                                        + contents.get(i).getDetail().getKey()
                                        + END_CHARACTER,
                                subs);
            }
        }

        templateWithProcessedPlaceholders = returnedString;

        log.debug("Substituted value: {}", returnedString);
        return returnedString;
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
            if (contents.get(i).equals(values)) {
                String sub = contents.get(i).substitute(value);

                log.debug("Substituted value into template: {}", contents.get(i).toString());

                // replace the template placeholder with the template value
                returnedString =
                        returnedString.replace(
                                START_CHARACTER
                                        + contents.get(i).getDetail().getKey()
                                        + END_CHARACTER,
                                sub);

                log.debug("Substituted value into the sentence: {}", contents.get(i).toString());

                // now copy the exists sentence but remove the substituted template
                WordingTemplateSentence newCollection =
                        new WordingTemplateSentence(this, returnedString, contents);

                // remove the already substituted template from the collection
                newCollection.contents.remove(contents.get(i));

                log.debug(
                        "Returning a new sentence with the template replaced : {}",
                        newCollection.toString());

                return newCollection;
            }
        }

        // if all else fails return this sentence
        return this;
    }

    @Override
    public String getSubstitutedSentence() {
        return templateWithProcessedPlaceholders;
    }

    @Override
    public Templateable getTemplateForReference(String referenceValue) {
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i).getDetail().getKey().equals(referenceValue)) {
                return contents.get(i);
            }
        }

        return null;
    }

    /** A wording template that supports substitution. */
    @ToString
    public static class WordingTemplate implements Templateable {
        /** Then delimiter. */
        private static String DELIMITER = "\\|";

        /** The regex pattern to split the template. */
        private static final Pattern PATTERN =
                Pattern.compile(
                        "[^|]+\\" + DELIMITER + "[^|]+\\" + DELIMITER + "[^}]+", Pattern.DOTALL);

        /** The template string. */
        private TemplateKeyWithConstraint templateKeyWithConstraint;

        @Override
        public TemplateKeyWithConstraint getDetail() {
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

        public static DataType validateDataType(String type) {
            // check the data types is correct in the template
            for (WordingDataTypes types : WordingDataTypes.values()) {
                if (types.getValue().equals(type)) {
                    return types.getType();
                }
            }

            return null;
        }

        /**
         * splits the pattern into parts.
         *
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
         * substitute the text into the template.
         *
         * @param values The list of options to substitute
         * @return The substituted string or not present if validation failed. NOTE: This method
         *     simply returns the original string if substitution can be performed
         */
        public String substitute(String values) {
            canValueBeSubstituted(values);
            return values;
        }
    }
}
