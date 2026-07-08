package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.template.BraceSubstitutedSentence;
import uk.gov.hmcts.appregister.common.template.Templateable;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;

/**
 * A common wording template mapper.
 */
@Slf4j
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public class WordingTemplateMapper {

    /**
     * gets a template detail from the application list entry containing the values.
     *
     * @param wordingTemplateSupplier The wording template
     * @param appliedTemplateSupplier The wording template that has been applied, with values
     *     substituted. This is optional as the template may not have been applied yet, in which
     *     case it can be null
     * @return The template details
     */
    public TemplateDetail getTemplateDetail(
            Supplier<String> wordingTemplateSupplier, Supplier<String> appliedTemplateSupplier) {
        return getTemplateDetail(wordingTemplateSupplier, appliedTemplateSupplier, true);
    }

    private TemplateDetail getTemplateDetail(
            Supplier<String> wordingTemplateSupplier,
            Supplier<String> appliedTemplateSupplier,
            boolean validateStoredValues) {
        log.debug("Parsing template {}", wordingTemplateSupplier.get());

        WordingTemplateSentence wordingTemplate =
                WordingTemplateSentence.with(wordingTemplateSupplier.get());

        // ensure we return the values if we have applied the template
        // else just return the parsed template details
        if (appliedTemplateSupplier != null) {
            log.debug("Parsing applied template");

            // parse out the wording string assuming braces delimet each value
            BraceSubstitutedSentence sentence =
                    BraceSubstitutedSentence.withSubstitutedSentence(appliedTemplateSupplier.get());

            List<String> appliedValues = sentence.getAppliedValues();
            Templateable[] templateables = wordingTemplate.getTemplateableContents();

            if (appliedValues.size() != templateables.length) {
                log.warn(
                        "Stored wording '{}' contains {} values but template '{}' expects {}."
                                + " Filling what we can and leaving the rest blank.",
                        appliedTemplateSupplier.get(),
                        appliedValues.size(),
                        wordingTemplateSupplier.get(),
                        templateables.length);
            }

            if (validateStoredValues) {
                for (int i = 0; i < templateables.length; i++) {
                    String value = i < appliedValues.size() ? appliedValues.get(i) : "";
                    wordingTemplate.substituteForTemplate(templateables[i], value);
                }

                // gets the template details with the values that are currently in the database
                // for each key
                return wordingTemplate.getDetail();
            }

            TemplateDetail detail = wordingTemplate.getDetail();
            for (int i = 0; i < detail.getSubstitutionKeyConstraints().size(); i++) {
                String value = i < appliedValues.size() ? appliedValues.get(i) : "";
                detail.getSubstitutionKeyConstraints().get(i).setValue(value);
            }

            return detail;
        } else {
            log.debug("No applied values to parse");

            return wordingTemplate.getDetail();
        }
    }

    /**
     * gets template detail for stored applied wording without re-validating legacy values against
     * current write-time constraints.
     *
     * @param wordingTemplateSupplier The wording template
     * @param appliedTemplateSupplier The stored applied wording
     * @return The template details
     */
    public TemplateDetail getStoredTemplateDetail(
            Supplier<String> wordingTemplateSupplier, Supplier<String> appliedTemplateSupplier) {
        return getTemplateDetail(wordingTemplateSupplier, appliedTemplateSupplier, false);
    }
}
