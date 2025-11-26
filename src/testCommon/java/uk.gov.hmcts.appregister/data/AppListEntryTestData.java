package uk.gov.hmcts.appregister.data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;

import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import static org.instancio.Select.field;

public class AppListEntryTestData
        implements uk.gov.hmcts.appregister.testutils.data.Persistable<
                ApplicationListEntry, ApplicationListEntry.ApplicationListEntryBuilder> {

    public static final String TRUE_VALUE = "1";

    @Override
    public ApplicationListEntry.ApplicationListEntryBuilder someMinimal() {
        UUID uniqueId = UUID.randomUUID();
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationCode code = new ApplicationCodeTestData().someComplete();
        return ApplicationListEntry.builder()
                .applicationCode(code)
                .applicationList(list)
                .applicationListEntryWording("Wording " + uniqueId)
                .entryRescheduled(TRUE_VALUE)
                .sequenceNumber(Short.MIN_VALUE)
                .lodgementDate(LocalDate.now(ZoneId.of("UTC")))
                .entryFeeIds(List.of(new AppListEntryFeeIdTestData().someComplete()));
    }

    public ApplicationListEntry createApplicationListEntry(
            ApplicationList list, Short sequenceNumber) {
        ApplicationListEntry listEntryData = new AppListEntryTestData().someMinimal().build();

        listEntryData.setApplicationList(list);
        listEntryData.setSequenceNumber(sequenceNumber);
        return listEntryData;
    }

    @Override
    public ApplicationListEntry someComplete() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        return Instancio.of(ApplicationListEntry.class)
                .ignore(field(AppListEntryFeeStatus::getId))
                .ignore(field(AppListEntryFeeStatus::getVersion))
                .withSettings(settings)
                .create();
    }
}
