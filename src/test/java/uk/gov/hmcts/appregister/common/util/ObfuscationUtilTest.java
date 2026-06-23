package uk.gov.hmcts.appregister.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.report.service.ReportDownload;

class ObfuscationUtilTest {

    @Test
    void testObfuscationAppListEntity() {
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();
        Assertions.assertEquals(
                2,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(appListEntryTestData.someComplete()),
                        "[REDACTED]"));
    }

    @Test
    void testObfuscationNameAddress() {
        NameAddress nameAddress = new NameAddress();
        Assertions.assertEquals(
                1,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(nameAddress), "[REDACTED]"));
    }

    @Test
    void testObfuscationEntryGetDetailDto() {
        EntryGetDetailDto entryGetDetailDto = Instancio.of(EntryGetDetailDto.class).create();
        Assertions.assertEquals(
                1,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(entryGetDetailDto), "[REDACTED]"));
    }

    @Test
    void testObfuscationEntryPage() {
        EntryPage entryPage = Instancio.of(EntryPage.class).create();

        EntryGetSummaryDto entryGetSummaryDto = entryPage.getContent().get(0);
        entryPage.getContent().clear();
        entryPage.getContent().add(entryGetSummaryDto);
        entryGetSummaryDto.accountNumber("ACC-12345");

        String obfuscated = ObfuscationUtil.getObfuscatedString(entryPage);

        assertThat(obfuscated)
                .doesNotContain("ACC-12345")
                .contains("\"applicant\":\"[REDACTED]\"")
                .contains("\"respondent\":\"[REDACTED]\"")
                .contains("\"accountNumber\":\"[REDACTED]\"");
    }

    @Test
    void testObfuscationStandardApplicantGetSummaryDto() {
        StandardApplicantGetSummaryDto standardApplicantGetSummaryDto =
                Instancio.of(StandardApplicantGetSummaryDto.class).create();
        Assertions.assertEquals(
                2,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(standardApplicantGetSummaryDto),
                        "[REDACTED]"));
    }

    @Test
    void testObfuscationEntryGetPrintDto() {
        EntryGetPrintDto entryGetPrintDto = Instancio.of(EntryGetPrintDto.class).create();

        Assertions.assertEquals(
                1,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(entryGetPrintDto), "[REDACTED]"));
    }

    @Test
    void testObfuscationApplicationListEntrySummary() {
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

        assertThat(obfuscated)
                .doesNotContain("ACC-12345")
                .doesNotContain("Applicant Name")
                .doesNotContain("Respondent Name")
                .doesNotContain("SW1A 2AA")
                .contains("\"accountNumber\":\"[REDACTED]\"")
                .contains("\"applicant\":\"[REDACTED]\"")
                .contains("\"respondent\":\"[REDACTED]\"")
                .contains("\"postCode\":\"[REDACTED]\"")
                .contains("\"applicationTitle\":\"Application title\"");
    }

    @Test
    void testObfuscationApplicationListGetDetailDto() {
        ApplicationListGetDetailDto dto =
                new ApplicationListGetDetailDto()
                        .id(UUID.randomUUID())
                        .date(LocalDate.of(2026, Month.MAY, 26))
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
                        .entriesCount(1);

        String obfuscated = ObfuscationUtil.getObfuscatedString(dto);

        assertThat(obfuscated)
                .doesNotContain("ACC-67890")
                .doesNotContain("Jane Applicant")
                .doesNotContain("John Respondent")
                .doesNotContain("EC1A 1BB")
                .contains("\"description\":\"Morning list\"")
                .doesNotContain("\"entriesSummary\"");
    }

    @Test
    void testObfuscationResultGetDto() {
        ResultGetDto resultGetDto =
                new ResultGetDto()
                        .id(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .resultCode("RC-001")
                        .wording(Instancio.of(TemplateDetail.class).create());

        String obfuscated = ObfuscationUtil.getObfuscatedString(resultGetDto);

        assertThat(obfuscated)
                .contains("\"resultCode\":\"RC-001\"")
                .contains("\"entryId\"")
                .contains("\"wording\"");
    }

    @Test
    void testObfuscationResultPage() {
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

        assertThat(obfuscated)
                .contains("\"content\"")
                .contains("\"resultCode\":\"RC-002\"")
                .contains("\"pageNumber\":0")
                .contains("\"totalElements\":1");
    }

    @Test
    void testObfuscationFeesReportFilterDto() {
        String standardApplicantCode = "STD-00123";
        String applicantName = "john smith";

        FeesReportFilterDto filterDto =
                new FeesReportFilterDto()
                        .standardApplicantCode(standardApplicantCode)
                        .applicantName(applicantName)
                        .location(null);

        String obfuscated = ObfuscationUtil.getObfuscatedString(filterDto);

        assertThat(obfuscated)
                .doesNotContain(standardApplicantCode)
                .doesNotContain(applicantName)
                .contains("\"standardApplicantCode\":\"[REDACTED]\"")
                .contains("\"applicantName\":\"[REDACTED]\"");
    }

    @Test
    void testObfuscationFeesReportFilterDtoRequiredOnly() {
        var today = LocalDate.of(2026, Month.JUNE, 17);
        FeesReportFilterDto filterDto = new FeesReportFilterDto().dateTo(today).dateFrom(today);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'['yyyy,M,dd']'");
        String obfuscated = ObfuscationUtil.getObfuscatedString(filterDto);
        assertThat(obfuscated)
                .contains("\"dateTo\":" + formatter.format(today))
                .contains("\"dateFrom\":" + formatter.format(today))
                .doesNotContain("\"standardApplicantCode\":\"[REDACTED]\"")
                .doesNotContain("\"applicantName\":\"[REDACTED]\"")
                .doesNotContain("\"location\"");
    }

    @Test
    void testObfuscationPrivateProsecutorIndexFilterDto() {
        String standardApplicantName = "Test Standard Applicant";
        String applicantFirstName = "john";
        String applicantSurname = "smith";
        String applicantOrganisationName = "Acme Corp";
        String respondentFirstName = "jane";
        String respondentSurname = "doe";
        String respondentOrganisationName = "Beta Ltd";

        PrivateProsecutorsIndexFilterDto filterDto =
                new PrivateProsecutorsIndexFilterDto()
                        .standardApplicantName(standardApplicantName)
                        .applicantSurname(applicantSurname)
                        .applicantFirstName(applicantFirstName)
                        .applicantOrganisationName(applicantOrganisationName)
                        .respondentSurname(respondentSurname)
                        .respondentFirstName(respondentFirstName)
                        .respondentOrganisationName(respondentOrganisationName)
                        .location(null);

        String obfuscated = ObfuscationUtil.getObfuscatedString(filterDto);

        assertThat(obfuscated)
                .doesNotContain(standardApplicantName)
                .doesNotContain(applicantFirstName)
                .doesNotContain(applicantSurname)
                .doesNotContain(applicantOrganisationName)
                .doesNotContain(respondentSurname)
                .doesNotContain(respondentFirstName)
                .doesNotContain(respondentOrganisationName)
                .doesNotContain("location")
                .contains("\"standardApplicantName\":\"[REDACTED]\"")
                .contains("\"applicantFirstName\":\"[REDACTED]\"")
                .contains("\"applicantSurname\":\"[REDACTED]\"")
                .contains("\"applicantOrganisationName\":\"[REDACTED]\"")
                .contains("\"respondentFirstname\":\"[REDACTED]\"")
                .contains("\"respondentSurname\":\"[REDACTED]\"")
                .contains("\"respondentOrganisationName\":\"[REDACTED]\"")
                .doesNotContain("\"location\"");
    }

    @Test
    void testObfuscationPrivateProsecutorIndexFilterDtoRequiredOnly() {
        var today = LocalDate.of(2026, Month.JUNE, 17);
        PrivateProsecutorsIndexFilterDto filterDto =
                new PrivateProsecutorsIndexFilterDto().dateTo(today).dateFrom(today);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'['yyyy,M,dd']'");
        String obfuscated = ObfuscationUtil.getObfuscatedString(filterDto);
        assertThat(obfuscated)
                .contains("\"dateTo\":" + formatter.format(today))
                .contains("\"dateFrom\":" + formatter.format(today))
                .doesNotContain("\"standardApplicantName\":\"[REDACTED]\"")
                .doesNotContain("\"applicantFirstName\":\"[REDACTED]\"")
                .doesNotContain("\"applicantSurname\":\"[REDACTED]\"")
                .doesNotContain("\"applicantOrganisationName\":\"[REDACTED]\"")
                .doesNotContain("\"respondentFirstname\":\"[REDACTED]\"")
                .doesNotContain("\"respondentSurname\":\"[REDACTED]\"")
                .doesNotContain("\"respondentOrganisationName\":\"[REDACTED]\"")
                .doesNotContain("\"location\"");
    }

    @Test
    void testObfuscationReportDownloadDoesNotConsumeStream() throws Exception {
        var resource =
                new InputStreamResource(
                        new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));
        var reportDownload = new ReportDownload("report.csv", resource);

        String obfuscated = ObfuscationUtil.getObfuscatedString(reportDownload);

        assertThat(obfuscated)
                .contains("\"filename\":\"report.csv\"")
                .contains("\"resource\":\"[REDACTED]\"");
        Assertions.assertArrayEquals(
                "report".getBytes(StandardCharsets.UTF_8),
                resource.getInputStream().readAllBytes());
    }
}
