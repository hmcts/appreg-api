package uk.gov.hmcts.appregister.common.mapper;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.generated.model.TemplateKeyWithConstraint;

public class WordingSubstitutionKeyExtractor {

    @NotNull
    public static List<String> getStrings(String wording) {
        if (wording == null) {
            return List.of();
        }

        ArrayList<String> retKeys = new ArrayList<>();
        TemplateDetail templateDetail = WordingTemplateSentence.with(wording).getDetail();
        if (templateDetail.getSubstitutionKeyConstraints() != null) {
            for (TemplateKeyWithConstraint substitution :
                    templateDetail.getSubstitutionKeyConstraints()) {
                retKeys.add(substitution.getKey());
            }
        }

        return retKeys;
    }
}
