package uk.gov.hmcts.appregister.util.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.asseertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationWordingParserTest {

    private WordingTemplateParser parser;

    @BeforeEach
    void setUp() {
        parser = new WordingTemplateParser();
    }

    @Test
    void generateWording_validInput_returnsReplacedTemplate() {
        String template = "The hearing is on {TEXT|Date|10} at {TEXT|Time|5}.";
        List<String> input = Arrays.asList("2025-06-01", "09:00");
        String result = parser.generateWording(template, input);
        assertEquals("The hearing is on 2025-06-01 at 09:00.", result);
    }

    @Test
    void generateWording_notEnoughInputFields_throwsException() {
        String template = "The hearing is on {TEXT|Date|10} at {TEXT|Time|5}.";
        List<String> input = Collections.singletonList("2025-06-01");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            parser.generateWording(template, input)
        );
        assertTrue(ex.getMessage().contains("Expected"));
    }

    @Test
    void generateWording_inputFieldExceedsLength_throwsException() {
        String template = "The hearing is on {TEXT|Date|10}.";
        List<String> input = Collections.singletonList("2025-06-01-TOO-LONG");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            parser.generateWording(template, input)
        );
        assertTrue(ex.getMessage().contains("exceed"));
    }

    @Test
    void generateWording_extraInputFields_ignoresExtras() {
        String template = "Filed by {TEXT|User|8}.";
        List<String> input = Arrays.asList("Alice", "ExtraIgnored");
        String result = parser.generateWording(template, input);
        assertEquals("Filed by Alice.", result);
    }

    @Test
    void generateWording_nullInputFields_throwsException() {
        String template = "Filed by {TEXT|User|8}.";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            parser.generateWording(template, null)
        );
        assertTrue(ex.getMessage().contains("text fields"));
    }

    @Test
    void generateWording_emptyTemplate_returnsSameString() {
        String result = parser.generateWording("Nothing to replace here.", Collections.emptyList());
        assertEquals("Nothing to replace here.", result);
    }

    @Test
    void generateWording_templateWithNoTokens_worksWithoutError() {
        String template = "No tokens present.";
        List<String> input = Arrays.asList("Should", "Be", "Ignored");
        String result = parser.generateWording(template, input);
        assertEquals("No tokens present.", result);
    }
}
