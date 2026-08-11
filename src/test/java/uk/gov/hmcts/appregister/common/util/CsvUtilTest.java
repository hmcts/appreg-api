package uk.gov.hmcts.appregister.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CsvUtilTest {

    @Test
    void testEscapeCharactersAcceptedSymbols() {
        // Ensuring if the string contains any of the accepted symbols, it is not escaped
        Assertions.assertEquals("Hello, World!", CsvUtil.escapeCharacters("Hello, World!"));
    }

    @Test
    void testEscapeCharactersWithFormularSymbolsAtBeginning() {
        // Should all be escaped as they contain a formular symbol.
        Assertions.assertEquals("'=1+1", CsvUtil.escapeCharacters("=1+1"));
        Assertions.assertEquals("'+Sheet1", CsvUtil.escapeCharacters("+Sheet1"));
        Assertions.assertEquals("'@username", CsvUtil.escapeCharacters("@username"));
        Assertions.assertEquals("'-comment", CsvUtil.escapeCharacters("-comment"));
    }

    @Test
    void testEscapeCharactersWithFormularSymbolsAtEnd() {
        // Should not be escaped as they are not at the beginning of the string
        Assertions.assertEquals("1+1=", CsvUtil.escapeCharacters("1+1="));
        Assertions.assertEquals("Sheet1+", CsvUtil.escapeCharacters("Sheet1+"));
        Assertions.assertEquals("Sheet1-", CsvUtil.escapeCharacters("Sheet1-"));
        Assertions.assertEquals("Sheet1@", CsvUtil.escapeCharacters("Sheet1@"));
    }

    @Test
    void testEscapeCharactersWithFormularSymbolsInMiddle() {
        // Should not be escaped as they are not at the beginning of the string
        Assertions.assertEquals("1=1", CsvUtil.escapeCharacters("1=1"));
        Assertions.assertEquals("She+et1", CsvUtil.escapeCharacters("She+et1"));
        Assertions.assertEquals("She-et1", CsvUtil.escapeCharacters("She-et1"));
        Assertions.assertEquals("She@et1", CsvUtil.escapeCharacters("She@et1"));
    }

    @Test
    void testEscapeCsvRowsContainingFormularSymbols() {
        String csv = "Name|Value\n=1+1|10\n+Sheet1|20\n@username|30\n-comment|40";
        String expectedEscapedCsv =
                "Name|Value\n'=1+1|10\n'+Sheet1|20\n'@username|30\n'-comment|40";
        Assertions.assertEquals(expectedEscapedCsv, CsvUtil.escapeCSV(csv));
    }

    @Test
    void testEscapeCsvRowsWithoutFormularSymbols() {
        String csv = "Name|Value\nHello|10\nWorld|20";
        String expectedEscapedCsv = "Name|Value\nHello|10\nWorld|20";
        Assertions.assertEquals(expectedEscapedCsv, CsvUtil.escapeCSV(csv));
    }

    @Test
    void testEscapeCsvRowsWithEmptyRows() {
        String csv = "Name|Value\nHello|10\n\nWorld|20";
        String expectedEscapedCsv = "Name|Value\nHello|10\n\nWorld|20";
        Assertions.assertEquals(expectedEscapedCsv, CsvUtil.escapeCSV(csv));
    }

    @Test
    void testEscapeCsvRowsWithEmptyColumns() {
        String csv = "Name|Value\nHello||20";
        String expectedEscapedCsv = "Name|Value\nHello||20";
        Assertions.assertEquals(expectedEscapedCsv, CsvUtil.escapeCSV(csv));
    }

    @Test
    void testEscapeCsvRowsWithEmptyRowsAndColumns() {
        String csv = "Name|Value\nHello||\n\nWorld|20";
        String expectedEscapedCsv = "Name|Value\nHello||\n\nWorld|20";
        Assertions.assertEquals(expectedEscapedCsv, CsvUtil.escapeCSV(csv));
    }
}
