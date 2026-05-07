package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import static org.instancio.Select.field;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

public class ApplicationListEntryResultEntityMapperTest {

    @Test
    void testToApplicationListEntryResultForCreate() {
        ApplicationListEntryResultEntityMapper applicationListEntryResultEntityMapper =
                new ApplicationListEntryResultEntityMapperImpl();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ResultCreateDto resultCreateDto =
                Instancio.of(ResultCreateDto.class).withSettings(settings).create();

        String substituteWording = "substituteWording";
        ResolutionCode resolutionCode =
                Instancio.of(ResolutionCode.class).withSettings(settings).create();

        ApplicationListEntry applicationListEntry =
                Instancio.of(ApplicationListEntry.class)
                        .ignore(field(ApplicationListEntry::getId))
                        .ignore(field(ApplicationListEntry::getVersion))
                        .withSettings(settings)
                        .create();

        String resolutionOfficer = "resolutionOfficer";

        AppListEntryResolution appListEntryResolution =
                applicationListEntryResultEntityMapper.toApplicationListEntryResult(
                        resultCreateDto,
                        substituteWording,
                        resolutionCode,
                        applicationListEntry,
                        resolutionOfficer);

        Assertions.assertEquals(applicationListEntry, appListEntryResolution.getApplicationList());
        Assertions.assertEquals(substituteWording, appListEntryResolution.getResolutionWording());
        Assertions.assertEquals(resolutionCode, appListEntryResolution.getResolutionCode());
        Assertions.assertEquals(resolutionOfficer, appListEntryResolution.getResolutionOfficer());
        Assertions.assertEquals(applicationListEntry, appListEntryResolution.getApplicationList());
        Assertions.assertNull(appListEntryResolution.getCreatedUser());
        Assertions.assertNull(appListEntryResolution.getChangedBy());
        Assertions.assertNull(appListEntryResolution.getId());
        Assertions.assertNull(appListEntryResolution.getVersion());
        Assertions.assertNull(appListEntryResolution.getUuid());
    }

    @Test
    void testToApplicationListEntryResultForUpdate() {
        ApplicationListEntryResultEntityMapper applicationListEntryResultEntityMapper =
                new ApplicationListEntryResultEntityMapperImpl();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ResultUpdateDto resultUpdateDto =
                Instancio.of(ResultUpdateDto.class).withSettings(settings).create();

        String substituteWording = "substituteWording";
        ResolutionCode resolutionCode =
                Instancio.of(ResolutionCode.class).withSettings(settings).create();

        ApplicationListEntry applicationListEntry =
                Instancio.of(ApplicationListEntry.class)
                        .ignore(field(ApplicationListEntry::getId))
                        .ignore(field(ApplicationListEntry::getVersion))
                        .withSettings(settings)
                        .create();

        String resolutionOfficer = "resolutionOfficer";

        AppListEntryResolution resolution = new AppListEntryResolution();

        applicationListEntryResultEntityMapper.toApplicationListEntryResult(
                resultUpdateDto,
                substituteWording,
                resolutionCode,
                applicationListEntry,
                resolutionOfficer,
                resolution);

        Assertions.assertEquals(applicationListEntry, resolution.getApplicationList());
        Assertions.assertEquals(substituteWording, resolution.getResolutionWording());
        Assertions.assertEquals(resolutionCode, resolution.getResolutionCode());
        Assertions.assertEquals(resolutionOfficer, resolution.getResolutionOfficer());
        Assertions.assertEquals(applicationListEntry, resolution.getApplicationList());
        Assertions.assertNull(resolution.getCreatedUser());
        Assertions.assertNull(resolution.getChangedBy());
        Assertions.assertNull(resolution.getId());
        Assertions.assertNull(resolution.getVersion());
        Assertions.assertNull(resolution.getUuid());
        Assertions.assertNull(resolution.getChangedDate());
    }
}
