package uk.gov.hmcts.appregister.applicationlist.mapper;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.util.TestConstants.CJA1_CODE;
import static uk.gov.hmcts.appregister.util.TestConstants.CJA1_DESCRIPTION;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;

class ApplicationListMappingHelperTest {

    @Test
    void testFormatDurationWithNullDurationReturnsNull() {
        Assertions.assertNull(ApplicationListMappingHelper.formatDuration(null));
    }

    @Test
    void testFormatDurationWithHoursAndMinutes() {
        ApplicationList app = Mockito.mock(ApplicationList.class);
        when(app.getDurationHours()).thenReturn((short) 1);
        when(app.getDurationMinutes()).thenReturn((short) 30);

        String result = ApplicationListMappingHelper.formatDuration(app);

        Assertions.assertEquals("1 Hours 30 Minutes", result);
    }

    @Test
    void testFormatDurationWithZeroHours() {
        ApplicationList app = Mockito.mock(ApplicationList.class);
        when(app.getDurationHours()).thenReturn((short) 0);
        when(app.getDurationMinutes()).thenReturn((short) 5);

        String result = ApplicationListMappingHelper.formatDuration(app);

        Assertions.assertEquals("0 Hours 5 Minutes", result);
    }

    @Test
    void testFormatCjaWithNullCjaReturnsNull() {
        Assertions.assertNull(ApplicationListMappingHelper.formatCja(null));
    }

    @Test
    void testFormatCjaWithNullFieldsReturnsNull() {
        CriminalJusticeArea cja = Mockito.mock(CriminalJusticeArea.class);
        when(cja.getCode()).thenReturn(null);
        when(cja.getDescription()).thenReturn(null);

        Assertions.assertNull(ApplicationListMappingHelper.formatCja(cja));
    }

    @Test
    void testFormatCjaWithNullCodeReturnsDescription() {
        CriminalJusticeArea cja = Mockito.mock(CriminalJusticeArea.class);
        when(cja.getCode()).thenReturn(null);
        when(cja.getDescription()).thenReturn(CJA1_DESCRIPTION);

        Assertions.assertEquals(CJA1_DESCRIPTION, ApplicationListMappingHelper.formatCja(cja));
    }

    @Test
    void testFormatCjaWithNullDescriptionReturnsCode() {
        CriminalJusticeArea cja = Mockito.mock(CriminalJusticeArea.class);
        when(cja.getCode()).thenReturn(CJA1_CODE);
        when(cja.getDescription()).thenReturn(null);

        Assertions.assertEquals(CJA1_CODE, ApplicationListMappingHelper.formatCja(cja));
    }

    @Test
    void testFormatCjaWithCodeAndDescription() {
        CriminalJusticeArea cja = Mockito.mock(CriminalJusticeArea.class);
        when(cja.getCode()).thenReturn(CJA1_CODE);
        when(cja.getDescription()).thenReturn(CJA1_DESCRIPTION);

        Assertions.assertEquals(
                CJA1_CODE + " - " + CJA1_DESCRIPTION, ApplicationListMappingHelper.formatCja(cja));
    }
}
