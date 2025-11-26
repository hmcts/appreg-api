package uk.gov.hmcts.appregister.common.template.wording;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.template.Templateable;
import uk.gov.hmcts.appregister.common.template.TemplateableSentence;
import uk.gov.hmcts.appregister.common.template.type.DataType;
import uk.gov.hmcts.appregister.common.util.ReadOnlyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that allows us to parse a Wording Templates sentence containing multiple work templates of the form {TYPE|REFERENCE|LENGTH}
 * and substitute values into it.
 *
 * Each template needs to have the following string format:=
 *
 * TYPE - The data type (e.g. TEXT)
 * REFERENCE - The reference name for the data
 * LENGTH - The length of the data
 * E.g. {TEXT|Applicant Name|50}
 */
@Slf4j
@ToString
public class WordingTemplateSentence extends ReadOnlyList<Templateable> implements TemplateableSentence {
    /** The starting character */
    private static final String START_CHARACTER = "{";

    /** The end character */
    private static final String END_CHARACTER = "}";

    /** The original template string */
    private String template;

    /** The erroneous templates that have been identified */
    private List<String> erroneous = new ArrayList<>();

    /** The template string with placeholders */
    private String templateWithPositionalPlaceholders = "";

    /** The placeholder UUID that is used as a unique placeholder with the template. Without this
     * we can not guarantee a unique substitution key */
    private UUID positionalPlaceholderPrefix = UUID.randomUUID();

    /** The regular expression to identify the template regex */
    private static final String TEMPLATE_REGEX = "\\" + START_CHARACTER + "(.*?)\\" + END_CHARACTER;

    public WordingTemplateSentence(String templateString) {
        super(new ArrayList<>());
        this.template = templateString;
        templateWithPositionalPlaceholders = template;
        Pattern p = Pattern.compile(TEMPLATE_REGEX, Pattern.DOTALL);
        Matcher m = p.matcher(templateString);

        log.debug("Parsing wording template: {}", templateString);

        int positionIndex = 0;
        while (m.find()) {
            String grp = m.group(1);
            log.debug("Parsing wording template: {}", grp);

            try {
                WordingTemplate wordingTemplate = new WordingTemplate(grp);
                log.debug("Parsed wording template: {}", wordingTemplate.getReference());
                backing.add(wordingTemplate);

                // replace the pattern with a placeholder
                templateWithPositionalPlaceholders = templateWithPositionalPlaceholders
                    .replaceFirst("\\"
                                      + START_CHARACTER
                                      + Pattern.quote(grp) + "\\"
                                      + END_CHARACTER,
                                  getPlaceholderForPosition(positionIndex));
                positionIndex = positionIndex + 1;
            } catch (AppRegistryException ex) {
                log.warn("Failing to parse template %s".formatted(grp), ex);

                // store the erroneous template for reporting
                erroneous.add(grp);
            }
        }

        log.debug("Created template with positional placeholders: {}", templateWithPositionalPlaceholders);
    }

    /**
     * A constructor that copies an existing template collection but uses a different positional
     * template string
     * @param templateToCopy The template to copy
     * @param templateWithPlaceholders The processed placeholder string to work with
     */
    public WordingTemplateSentence(WordingTemplateSentence templateToCopy,
                                   String templateWithPlaceholders) {
        super(templateToCopy.backing);
        this.templateWithPositionalPlaceholders = templateWithPlaceholders;
        this.erroneous = templateToCopy.erroneous;
        this.positionalPlaceholderPrefix = templateToCopy.positionalPlaceholderPrefix;
        this.template = templateToCopy.template;
    }

    /**
     * gets the unique placeholder string in for a positional
     * @param position The position in the template
     * @return The unique placeholder for the id position
     */
    private String getPlaceholderForPosition(int position) {
        return positionalPlaceholderPrefix.toString() + position;
    }

    @Override
    public List<String> getReferences() {
        List<String> references = new ArrayList<>();
        for (int i=0;i < size(); i++) {
            references.add(get(i).getReference());
        }
        return references;
    }

    @Override
    public String getSentenceTemplate() {
        return template;
    }

    @Override
    public String substitute(List<String> values) {
        String returnedString = templateWithPositionalPlaceholders;

        if (values.size() > size()) {
            throw new AppRegistryException(CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                                           "Number of options exceeds number of templates",
                                           Map.of("templateSize", Integer.toString(size()),
                                                  "valueSize", Integer.toString(values.size())));
        }

        for (int i=0;i < values.size(); i++) {
            // if we have a value to substitute then substitute it into one of the valid templates
            log.debug("Substituting options into template: {}", get(i).toString());

            String subs = get(i).substitute(values.get(i));

            // replace the template placeholder with the template value
            returnedString = returnedString.replace(getPlaceholderForPosition(i), subs);
        }

        log.debug("Substituted value: {}", returnedString);
        return returnedString;
}

    /**
     * Creates a working template from a string.
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
        String returnedString = templateWithPositionalPlaceholders;


        // find the template to substitute
        for (int i=0;i < size(); i++) {

            // find the matching template reference
            if (get(i).equals(values)) {

                String sub = get(i).substitute(value);

                log.debug("Substituted value into template: {}", get(i).toString());

                // replace the template placeholder with the template value
                returnedString = returnedString.replace(getPlaceholderForPosition(i), sub);

                log.debug("Substituted value into the sentence: {}", get(i).toString());

                // now copy the exists sentence but remove the substituted template
                WordingTemplateSentence newCollection =  new WordingTemplateSentence(this, returnedString);

                // remove the already substituted template from the collection
                newCollection.backing.remove(newCollection.get(i));

                log.debug("Returning a new sentence with the template replaced : {}", newCollection.toString());

                return newCollection;
            }
        }

        // if all else fails return this sentence
        return this;
    }

    @Override
    public String getSubstitutedSentence() {
        return templateWithPositionalPlaceholders;
    }

    @Override
    public Templateable getTemplateForReference(String referenceValue) {
        for (int i=0;i < size(); i++) {
            if (get(i).getReference().equals(referenceValue)) {
                return get(i);
            }
        }

        return null;
    }

    /**
     * A wording template that supports substitution.
     */
    @ToString
    public static class WordingTemplate implements Templateable
    {
        /** Then delimiter */
        private static String DELIMITER = "\\|";

        /** The regex pattern to split the template */
        private static final Pattern PATTERN = Pattern.compile("[^|]+\\" + DELIMITER + "[^|]+\\"
                                                                   + DELIMITER + "[^}]+", Pattern.DOTALL);

        /** The reference that the substitute would happen */
        private String reference;

        /** The data type of the value */
        private DataType type;

        /** The length of the value */
        private int length;

        /**The template string */
        private String template;

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public DataType getType() {
            return type;
        }

        @Override
        public Integer getLength() {
            return length;
        }

        public WordingTemplate(String templateString) {
            // check the template string is valid
            if (!PATTERN.matcher(templateString).find()) {
                throw new AppRegistryException(CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            this.template = templateString;

            String[] parts = getPartsOfTemplate(templateString);

            // if the parts in the template do not equate to 3 then throw an exception
            if (parts.length != 3) {
                throw new AppRegistryException(CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE, "Invalid template string");
            }

            // split the template stringand store the meta data parts
            reference = parts[1];
            length = Integer.parseInt(parts[2]);

            // validates the data type
            type = validateDataType(parts[0]);

            if (type == null) {
                throw new AppRegistryException(CommonAppError.WORDING_DATA_TYPE_FAILURE, "Invalid data type in template");
            }
        }

        /**
         * Creates a working template from a string.
         * @param template The template string
         * @return wording template instance
         */
        public static WordingTemplate with(String template) {
            Pattern p = Pattern.compile(TEMPLATE_REGEX, Pattern.DOTALL);
            Matcher m = p.matcher(template);

            log.debug("Parsing wording template: {}", template);

            boolean found = m.find();
            if (!found) {
                throw new AppRegistryException(
                    CommonAppError.WORDING_TEMPLATE_FORMAT_FAILURE,
                    "Invalid template string"
                );
            }

            String grp = m.group(1);
            log.debug("Parsing wording template: {}", grp);

            return new WordingTemplate(grp);
        }

        private DataType validateDataType(String type) {
            WordingDataTypes matchingType = null;
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
         * @return The pattern parts
         */
        private String[] getPartsOfTemplate(String template) {
            return template.split(DELIMITER);
        }

        @Override
        public void canValueBeSubstituted(String value) {

            log.debug("Validating value '{}' for template: {}", value, this);
            if (!type.validateForType(value)) {
                throw new AppRegistryException(CommonAppError.WORDING_DATA_TYPE_FAILURE, "Invalid data type value in template",
                                               Map.of(getReference(), value));
            }

            log.debug("Validating value length'{}' for template: {}", value.length(), this);
            if (value.length() > length) {
                throw new AppRegistryException(CommonAppError.WORDING_LENGTH_FAILURE, "Invalid data type in template",
                                               Map.of(getReference(), value));
            }
        }

        /**
         * substitute the text into the template
         * @param values The list of options to substitute
         * @return The substituted string or not present if validation failed. NOTE: This method simply returns the
         * original string if substitution can be performed
         */
        public String substitute(String values) {
            canValueBeSubstituted(values);
            return values;
        }
    }
}
