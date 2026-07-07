package uk.gov.hmcts.appregister.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;

class WordingTemplateMapperTest {
    private static final String TEMPLATE_WORDING =
            "Attends to swear a complaint for the issue summonses for the debtors to answer "
                    + "an application for a liability order in relation to unpaid council tax "
                    + "(number of cases {TEXT|Number|4})";

    private static final String STAGED_ENTRY_WORDING = "Second application entry for list 101";

    private final WordingTemplateMapper wordingTemplateMapper = new WordingTemplateMapper();

    @Test
    void
            givenStoredWordingHasFewerPlaceholdersWhenGetTemplateDetailThenReturnsTemplateWithoutValues() {
        TemplateDetail detail =
                wordingTemplateMapper.getTemplateDetail(
                        () -> TEMPLATE_WORDING, () -> STAGED_ENTRY_WORDING);

        assertNotNull(detail);
        assertEquals(
                "Attends to swear a complaint for the issue summonses for the debtors to answer "
                        + "an application for a liability order in relation to unpaid council tax "
                        + "(number of cases {{Number}})",
                detail.getTemplate());
        assertEquals(1, detail.getSubstitutionKeyConstraints().size());
        assertEquals("Number", detail.getSubstitutionKeyConstraints().get(0).getKey());
        assertNull(detail.getSubstitutionKeyConstraints().get(0).getValue());
    }
}
