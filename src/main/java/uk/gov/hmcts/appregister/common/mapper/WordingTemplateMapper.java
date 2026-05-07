package uk.gov.hmcts.appregister.common.mapper;

import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.template.BraceSubstitutedSentence;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

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
        log.debug("Parsing template ", wordingTemplateSupplier.get());

        WordingTemplateSentence wordingTemplate =
                WordingTemplateSentence.with(wordingTemplateSupplier.get());

        // ensure we return the values if we have applied the template
        // else just return the parsed template details
        if (appliedTemplateSupplier != null) {
            log.debug("Parsing applied template");

            // parse out the wording string assuming braces delimet each value
            BraceSubstitutedSentence sentence =
                    BraceSubstitutedSentence.withSubstitutedSentence(appliedTemplateSupplier.get());

            // get substitution keys that need to be replaced
            List<TemplateSubstitution> keysForSubstitution =
                    wordingTemplate.getKeysToBeSubstituted();

            // apply the sentence value to substitution keys
            sentence.applyValuesTo(keysForSubstitution);

            // substitute using the keys
            wordingTemplate.substitute(keysForSubstitution);

            log.debug("Re-applied template values against template keys");

            // gets the template details with the values that are currently in the database
            // for each key
            return wordingTemplate.getDetail();
        } else {
            log.debug("No applied values to parse");

            return wordingTemplate.getDetail();
        }
    }
}
