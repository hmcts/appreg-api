package uk.gov.hmcts.appregister.applicationlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListDto;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

class ApplicationListValueTypesCoverageTest {

    @Test
    void applicationListDto_exposesRecordComponents() {
        var date = OffsetDateTime.parse("2026-06-11T10:15:30Z");
        var changedDate = OffsetDateTime.parse("2026-06-12T11:15:30Z");
        var courthouse = new CourtLocationGetDetailDto();
        var dto =
                new ApplicationListDto(
                        1L, "OPEN", date, "10:15", "List", courthouse, "tester", changedDate, 3);

        assertEquals(1L, dto.id());
        assertEquals("OPEN", dto.status());
        assertSame(date, dto.date());
        assertEquals("10:15", dto.time());
        assertEquals("List", dto.description());
        assertSame(courthouse, dto.courthouse());
        assertEquals("tester", dto.changedBy());
        assertSame(changedDate, dto.changedDate());
        assertEquals(3, dto.version());
    }

    @Test
    void applicationListWriteDto_exposesRecordComponents() {
        var date = OffsetDateTime.parse("2026-06-11T10:15:30Z");
        var time = OffsetDateTime.parse("2026-06-11T12:00:00Z");
        var dto = new ApplicationListWriteDto("OPEN", date, time, "List", 9L);

        assertEquals("OPEN", dto.status());
        assertSame(date, dto.date());
        assertSame(time, dto.time());
        assertEquals("List", dto.description());
        assertEquals(9L, dto.courthouseId());
    }

    @Test
    void standardApplicantDto_exposesRecordComponents() {
        var startDate = LocalDate.of(2026, Month.JANUARY, 1);
        var endDate = LocalDate.of(2026, Month.DECEMBER, 31);
        var dto =
                new StandardApplicantDto(
                        7L,
                        "CODE",
                        "Ms",
                        "Full Name",
                        "Forename1",
                        "Forename2",
                        "Forename3",
                        "Surname",
                        "Line1",
                        "Line2",
                        "Line3",
                        "Line4",
                        "Line5",
                        "AB1 2CD",
                        "user@example.com",
                        "01234",
                        "07890",
                        startDate,
                        endDate);

        assertEquals(7L, dto.id());
        assertEquals("CODE", dto.applicantCode());
        assertEquals("Ms", dto.applicantTitle());
        assertEquals("Full Name", dto.applicantName());
        assertEquals("Forename1", dto.applicantForename1());
        assertEquals("Forename2", dto.applicantForename2());
        assertEquals("Forename3", dto.applicantForename3());
        assertEquals("Surname", dto.applicantSurname());
        assertEquals("Line1", dto.addressLine1());
        assertEquals("Line2", dto.addressLine2());
        assertEquals("Line3", dto.addressLine3());
        assertEquals("Line4", dto.addressLine4());
        assertEquals("Line5", dto.addressLine5());
        assertEquals("AB1 2CD", dto.postcode());
        assertEquals("user@example.com", dto.emailAddress());
        assertEquals("01234", dto.telephoneNumber());
        assertEquals("07890", dto.mobileNumber());
        assertSame(startDate, dto.applicantStartDate());
        assertSame(endDate, dto.applicantEndDate());
    }
}
