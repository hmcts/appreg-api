package uk.gov.hmcts.appregister.applicationlist.mapper;

import lombok.experimental.UtilityClass;
import org.mapstruct.Named;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;

@UtilityClass
public final class ApplicationListMappingHelper {

    @Named("formatDuration")
    public static String formatDuration(ApplicationList app) {
        if (app == null) {
            return null;
        }
        ;

        short hours = app.getDurationHours();
        short minutes = app.getDurationMinutes();

        return hours + " Hours " + minutes + " Minutes";
    }

    @Named("formatCja")
    public static String formatCja(CriminalJusticeArea cja) {
        if (cja == null) {
            return null;
        }
        ;
        String code = cja.getCode();
        String desc = cja.getDescription();
        if (code == null && desc == null) {
            return null;
        }
        if (code == null) {
            return desc;
        }
        if (desc == null) {
            return code;
        }
        return code + " - " + desc;
    }
}
