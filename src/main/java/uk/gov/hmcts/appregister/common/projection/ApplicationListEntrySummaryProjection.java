package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

public interface ApplicationListEntrySummaryProjection {
    Long getId();

    UUID getUuid();

    short getSequenceNumber();

    String getAccountNumber();

    String getPostCode();

    String getApplicationTitle();

    boolean isFeeRequired();

    String getResult();

    UUID getListId();

    LocalDate getDate();

    StandardApplicant getStandardApplicant();

    NameAddress getApplicant();

    NameAddress getRespondent();
}
