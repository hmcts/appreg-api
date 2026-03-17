package uk.gov.hmcts.appregister.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.projection.ApplicationListSummaryProjection;

@Getter
@Setter
public class ApplicationListSummaryProjectionImpl implements ApplicationListSummaryProjection {
    private int entryCount;

    private UUID uuid;

    private String description;

    private String courtName;

    private String cjaDescription;

    private String otherLocation;

    private LocalDate date;

    private LocalTime time;

    private Status status;

    private String effectiveLocation;
}
