package uk.gov.hmcts.appregister.common.entity.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.enumeration.Status;

class ConverterCoverageTest {

    @Test
    void statusConverter_handlesNullAndRoundTrip() {
        var converter = new StatusConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("OPEN", converter.convertToDatabaseColumn(Status.OPEN));
        assertEquals(Status.CLOSED, converter.convertToEntityAttribute("CLOSED"));
    }

    @Test
    void feeStatusTypeConverter_handlesNullAndRoundTrip() {
        var converter = new FeeStatusTypeConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("P", converter.convertToDatabaseColumn(FeeStatusType.PAID));
        assertEquals(FeeStatusType.REMITTED, converter.convertToEntityAttribute("R"));
    }

    @Test
    void jobStatusConverter_handlesNullAndRoundTrip() {
        var converter = new JobStatusConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("FAILED", converter.convertToDatabaseColumn(JobStatusType.FAILED));
        assertEquals(JobStatusType.COMPLETED, converter.convertToEntityAttribute("COMPLETED"));
    }

    @Test
    void nameAddressConverter_handlesNullAndRoundTrip() {
        var converter = new NameAddressConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("NA", converter.convertToDatabaseColumn(NameAddressCodeType.APPLICANT));
        assertEquals(NameAddressCodeType.RESPONDENT, converter.convertToEntityAttribute("RE"));
    }

    @Test
    void officialConverter_handlesNullAndRoundTrip() {
        var converter = new OfficialConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals("M", converter.convertToDatabaseColumn(OfficialType.MAGISTRATE));
        assertEquals(OfficialType.CLERK, converter.convertToEntityAttribute("C"));
    }
}
