package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface ApplicationListEntrySummaryProjection {
    Long getId();

    UUID getUuid();

    short getSequenceNumber();

    String getAccountNumber();

    String getApplicant();

    String getRespondent();

    String getPostCode();

    String getApplicationTitle();

    boolean isFeeRequired();

    String getResult();

    UUID getApplicationListId();

    LocalDate getDate();
}
