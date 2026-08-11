package uk.gov.hmcts.appregister.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CsvUtilTest {

    @Test
    void testLeavesSafeValueUnchanged() {
        Assertions.assertEquals("Hello, World!", CsvUtil.escapeCharacters("Hello, World!"));
    }

    @Test
    void testEscapesFormulaSymbolsAtBeginning() {
        Assertions.assertEquals("'=1+1", CsvUtil.escapeCharacters("=1+1"));
        Assertions.assertEquals("'+Sheet1", CsvUtil.escapeCharacters("+Sheet1"));
        Assertions.assertEquals("'@username", CsvUtil.escapeCharacters("@username"));
        Assertions.assertEquals("'-comment", CsvUtil.escapeCharacters("-comment"));
    }

    @Test
    void testLeavesFormulaSymbolsAtEndUnchanged() {
        Assertions.assertEquals("1+1=", CsvUtil.escapeCharacters("1+1="));
        Assertions.assertEquals("Sheet1+", CsvUtil.escapeCharacters("Sheet1+"));
        Assertions.assertEquals("Sheet1-", CsvUtil.escapeCharacters("Sheet1-"));
        Assertions.assertEquals("Sheet1@", CsvUtil.escapeCharacters("Sheet1@"));
    }

    @Test
    void testLeavesFormulaSymbolsInMiddleUnchanged() {
        Assertions.assertEquals("1=1", CsvUtil.escapeCharacters("1=1"));
        Assertions.assertEquals("She+et1", CsvUtil.escapeCharacters("She+et1"));
        Assertions.assertEquals("She-et1", CsvUtil.escapeCharacters("She-et1"));
        Assertions.assertEquals("She@et1", CsvUtil.escapeCharacters("She@et1"));
    }

    @Test
    void testEscapeCharactersWithLeadingQuoteUnchanged() {
        var value = "\"=Not a formula";

        Assertions.assertEquals(value, CsvUtil.escapeCharacters(value));
    }

    @Test
    void testLeavesNullUnchanged() {
        Assertions.assertNull(CsvUtil.escapeCharacters(null));
    }
}
