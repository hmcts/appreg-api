package uk.gov.hmcts.appregister.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void givenStoredWordingHasFewerPlaceholdersWhenGetTemplateDetailThenFillsWhatItCan() {
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
        assertEquals("", detail.getSubstitutionKeyConstraints().get(0).getValue());
    }

    @Test
    void givenStoredWordingHasSomePlaceholdersWhenGetTemplateDetailThenLeavesTheRestBlank() {
        TemplateDetail detail =
                wordingTemplateMapper.getTemplateDetail(
                        () -> "Alpha {TEXT|First|10} Beta {TEXT|Second|10} Gamma {TEXT|Third|10}",
                        () -> "Alpha {one} Beta {two}");

        assertNotNull(detail);
        assertEquals("Alpha {{First}} Beta {{Second}} Gamma {{Third}}", detail.getTemplate());
        assertEquals(3, detail.getSubstitutionKeyConstraints().size());
        assertEquals("one", detail.getSubstitutionKeyConstraints().get(0).getValue());
        assertEquals("two", detail.getSubstitutionKeyConstraints().get(1).getValue());
        assertEquals("", detail.getSubstitutionKeyConstraints().get(2).getValue());
    }

    @Test
    void givenStoredWordingExceedsCurrentConstraintWhenGetTemplateDetailThenReturnStoredValue() {
        TemplateDetail detail =
                wordingTemplateMapper.getStoredTemplateDetail(
                        () -> "Legacy wording {TEXT|Reference|4}",
                        () -> "Legacy wording {E1234567890}");

        assertNotNull(detail);
        assertEquals("Legacy wording {{Reference}}", detail.getTemplate());
        assertEquals(1, detail.getSubstitutionKeyConstraints().size());
        assertEquals("Reference", detail.getSubstitutionKeyConstraints().get(0).getKey());
        assertEquals("E1234567890", detail.getSubstitutionKeyConstraints().get(0).getValue());
    }
}
