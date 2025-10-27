package uk.gov.hmcts.appregister.common.projection;

public interface ApplicationListEntrySummaryProjection {

    short getSequenceNumber();

    String getAccountNumber();

    String getApplicant();

    String getRespondent();

    String getPostCode();

    String getApplicationTitle();

    boolean isFeeRequired();

    String getResult();
}
