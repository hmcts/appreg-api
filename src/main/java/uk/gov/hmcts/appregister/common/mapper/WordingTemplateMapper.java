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
        if (appliedTemplateSupplier == null) {
            log.debug("No applied values to parse");
            return wordingTemplate.getDetail();
        }

        return buildAppliedTemplateDetail(
                wordingTemplateSupplier,
                appliedTemplateSupplier,
                validateStoredValues,
                wordingTemplate);
    }

    private TemplateDetail buildAppliedTemplateDetail(
            Supplier<String> wordingTemplateSupplier,
            Supplier<String> appliedTemplateSupplier,
            boolean validateStoredValues,
            WordingTemplateSentence wordingTemplate) {
        log.debug("Parsing applied template");

        var appliedTemplate = appliedTemplateSupplier.get();
        var appliedValues = parseAppliedValues(appliedTemplate);
        var templateables = wordingTemplate.getTemplateableContents();

        warnWhenAppliedValueCountDiffers(
                appliedTemplate, appliedValues, wordingTemplateSupplier.get(), templateables);

        if (validateStoredValues) {
            applyStoredValues(wordingTemplate, templateables, appliedValues);
            return wordingTemplate.getDetail();
        }

        return populateTemplateDetailValues(wordingTemplate.getDetail(), appliedValues);
    }

    private List<String> parseAppliedValues(String appliedTemplate) {
        return BraceSubstitutedSentence.withSubstitutedSentence(appliedTemplate).getAppliedValues();
    }

    private void warnWhenAppliedValueCountDiffers(
            String appliedTemplate,
            List<String> appliedValues,
            String wordingTemplate,
            Templateable[] templateables) {
        if (appliedValues.size() == templateables.length) {
            return;
        }

        log.warn(
                "Stored wording '{}' contains {} values but template '{}' expects {}."
                        + " Filling what we can and leaving the rest blank.",
                appliedTemplate,
                appliedValues.size(),
                wordingTemplate,
                templateables.length);
    }

    private void applyStoredValues(
            WordingTemplateSentence wordingTemplate,
            Templateable[] templateables,
            List<String> appliedValues) {
        for (int i = 0; i < templateables.length; i++) {
            wordingTemplate.substituteForTemplate(templateables[i], valueAt(appliedValues, i));
        }
    }

    private TemplateDetail populateTemplateDetailValues(
            TemplateDetail detail, List<String> appliedValues) {
        for (int i = 0; i < detail.getSubstitutionKeyConstraints().size(); i++) {
            detail.getSubstitutionKeyConstraints().get(i).setValue(valueAt(appliedValues, i));
        }

        return detail;
    }

    private String valueAt(List<String> appliedValues, int index) {
        return index < appliedValues.size() ? appliedValues.get(index) : "";
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
