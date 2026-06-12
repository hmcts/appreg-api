package uk.gov.hmcts.appregister.applicationlist.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;

@Component
public class ApplicationListMappingHelper {

    @Named("formatDuration")
    public String formatDuration(ApplicationList app) {
        if (app == null) {
            return null;
        }

        short hours = app.getDurationHours();
        short minutes = app.getDurationMinutes();

        return hours + " Hours " + minutes + " Minutes";
    }

    @Named("formatCja")
    public String formatCja(CriminalJusticeArea cja) {
        if (cja == null) {
            return null;
        }

        String code = cja.getCode();
        String description = cja.getDescription();

        if (code == null && description == null) {
            return null;
        }

        if (code == null) {
            return description;
        }

        if (description == null) {
            return code;
        }

        return code + " - " + description;
    }
}
