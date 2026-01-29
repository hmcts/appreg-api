package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.enumeration.Status;

public interface ApplicationListSummaryProjection {
    int getEntryCount();

    UUID getUuid();

    String getDescription();

    String getCourtName();

    String getCjaDescription();

    String getOtherLocation();

    LocalDate getDate();

    LocalTime getTime();

    Status getStatus();
}
