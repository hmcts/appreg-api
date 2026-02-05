package uk.gov.hmcts.appregister.common.util;

import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;

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
                4,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(entryGetDetailDto), "[REDACTED]"));
    }

    @Test
    public void testObfuscationEntryPage() {
        EntryPage entryPage = Instancio.of(EntryPage.class).create();

        EntryGetSummaryDto entryGetSummaryDto = entryPage.getContent().get(0);
        entryPage.getContent().clear();
        entryPage.getContent().add(entryGetSummaryDto);
        Assertions.assertEquals(
                4,
                StringUtils.countMatches(
                        ObfuscationUtil.getObfuscatedString(entryPage), "[REDACTED]"));
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
}
