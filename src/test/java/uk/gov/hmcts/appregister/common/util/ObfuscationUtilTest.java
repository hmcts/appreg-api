package uk.gov.hmcts.appregister.common.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;

public class ObfuscationUtilTest {

    @Test
    public void testObfuscationAppListEntity() {
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();
        Assertions.assertEquals(
                2,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(appListEntryTestData.someComplete()),
                        "[REDACTED]"));
    }

    @Test
    public void testObfuscationNameAddress() {
        NameAddress nameAddress = new NameAddress();
        Assertions.assertEquals(
                1,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(nameAddress), "[REDACTED]"));
    }

    @Test
    public void testObfuscationEntryGetDetailDto() {
        EntryGetDetailDto entryGetDetailDto = Instancio.of(EntryGetDetailDto.class).create();
        Assertions.assertEquals(
                1,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(entryGetDetailDto), "[REDACTED]"));
    }

    @Test
    public void testObfuscationEntryPage() {
        EntryPage entryPage = Instancio.of(EntryPage.class).create();

        EntryGetSummaryDto entryGetSummaryDto = entryPage.getContent().get(0);
        entryPage.getContent().clear();
        entryPage.getContent().add(entryGetSummaryDto);
        entryGetSummaryDto.accountNumber("ACC-12345");

        String obfuscated = ObfuscationUtil.getObfuscatedString(entryPage);

        Assertions.assertFalse(obfuscated.contains("ACC-12345"));
        Assertions.assertTrue(obfuscated.contains("\"applicant\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"respondent\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"accountNumber\":\"[REDACTED]\""));
    }

    @Test
    public void testObfuscationStandardApplicantGetSummaryDto() {
        StandardApplicantGetSummaryDto standardApplicantGetSummaryDto =
                Instancio.of(StandardApplicantGetSummaryDto.class).create();
        Assertions.assertEquals(
                2,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(standardApplicantGetSummaryDto),
                        "[REDACTED]"));
    }

    @Test
    public void testObfuscationEntryGetPrintDto() {
        EntryGetPrintDto entryGetPrintDto = Instancio.of(EntryGetPrintDto.class).create();

        Assertions.assertEquals(
                1,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(entryGetPrintDto), "[REDACTED]"));
    }

    @Test
    public void testObfuscationApplicationListEntrySummary() {
        ApplicationListEntrySummary summary =
                new ApplicationListEntrySummary()
                        .uuid(UUID.randomUUID())
                        .sequenceNumber(42)
                        .accountNumber("ACC-12345")
                        .applicant("Applicant Name")
                        .respondent("Respondent Name")
                        .postCode("SW1A 2AA")
                        .applicationTitle("Application title")
                        .feeRequired(true)
                        .result("Granted");

        String obfuscated = ObfuscationUtil.getObfuscatedString(summary);

        Assertions.assertFalse(obfuscated.contains("ACC-12345"));
        Assertions.assertFalse(obfuscated.contains("Applicant Name"));
        Assertions.assertFalse(obfuscated.contains("Respondent Name"));
        Assertions.assertFalse(obfuscated.contains("SW1A 2AA"));

        Assertions.assertTrue(obfuscated.contains("\"accountNumber\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"applicant\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"respondent\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"postCode\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"applicationTitle\":\"Application title\""));
    }

    @Test
    public void testObfuscationApplicationListGetDetailDto() {
        ApplicationListEntrySummary summary =
                new ApplicationListEntrySummary()
                        .uuid(UUID.randomUUID())
                        .sequenceNumber(7)
                        .accountNumber("ACC-67890")
                        .applicant("Jane Applicant")
                        .respondent("John Respondent")
                        .postCode("EC1A 1BB")
                        .applicationTitle("Detailed title")
                        .feeRequired(false)
                        .result("Refused");

        ApplicationListGetDetailDto dto =
                new ApplicationListGetDetailDto()
                        .id(UUID.randomUUID())
                        .date(LocalDate.of(2026, 5, 26))
                        .time(LocalTime.of(10, 30))
                        .description("Morning list")
                        .status(ApplicationListStatus.OPEN)
                        .courtCode("LOC123")
                        .courtName("Bath Magistrates Court")
                        .cjaCode("CJA001")
                        .otherLocationDescription("Temporary Courtroom")
                        .durationHours(2)
                        .durationMinutes(15)
                        .version(3L)
                        .entriesCount(1)
                        .entriesSummary(List.of(summary));

        String obfuscated = ObfuscationUtil.getObfuscatedString(dto);

        Assertions.assertFalse(obfuscated.contains("ACC-67890"));
        Assertions.assertFalse(obfuscated.contains("Jane Applicant"));
        Assertions.assertFalse(obfuscated.contains("John Respondent"));
        Assertions.assertFalse(obfuscated.contains("EC1A 1BB"));

        Assertions.assertTrue(obfuscated.contains("\"description\":\"Morning list\""));
        Assertions.assertTrue(obfuscated.contains("\"entriesSummary\""));
        Assertions.assertTrue(obfuscated.contains("\"accountNumber\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"applicant\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"respondent\":\"[REDACTED]\""));
        Assertions.assertTrue(obfuscated.contains("\"postCode\":\"[REDACTED]\""));
    }

    @Test
    public void testObfuscationResultGetDto() {
        ResultGetDto resultGetDto =
                new ResultGetDto()
                        .id(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .resultCode("RC-001")
                        .wording(Instancio.of(TemplateDetail.class).create());

        String obfuscated = ObfuscationUtil.getObfuscatedString(resultGetDto);

        Assertions.assertTrue(obfuscated.contains("\"resultCode\":\"RC-001\""));
        Assertions.assertTrue(obfuscated.contains("\"entryId\""));
        Assertions.assertTrue(obfuscated.contains("\"wording\""));
    }

    @Test
    public void testObfuscationResultPage() {
        ResultGetDto resultGetDto =
                new ResultGetDto()
                        .id(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .resultCode("RC-002")
                        .wording(Instancio.of(TemplateDetail.class).create());

        ResultPage resultPage =
                new ResultPage()
                        .pageNumber(0)
                        .pageSize(20)
                        .totalElements(1L)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .elementsOnPage(1)
                        .content(List.of(resultGetDto));

        String obfuscated = ObfuscationUtil.getObfuscatedString(resultPage);

        Assertions.assertTrue(obfuscated.contains("\"content\""));
        Assertions.assertTrue(obfuscated.contains("\"resultCode\":\"RC-002\""));
        Assertions.assertTrue(obfuscated.contains("\"pageNumber\":0"));
        Assertions.assertTrue(obfuscated.contains("\"totalElements\":1"));
    }
}
