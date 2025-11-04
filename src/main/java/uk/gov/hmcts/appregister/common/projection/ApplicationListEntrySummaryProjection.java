package uk.gov.hmcts.appregister.common.projection;

import java.util.UUID;

public interface ApplicationListEntrySummaryProjection {

    UUID getUuid();

    short getSequenceNumber();

    String getAccountNumber();

    String getApplicant();

    String getRespondent();

    String getPostCode();

    String getApplicationTitle();

    boolean isFeeRequired();

    String getResult();
}
