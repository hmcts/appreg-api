package uk.gov.hmcts.appregister.common.projection;

public interface ApplicationListEntrySummaryProjection {

    String getAccountNumber();

    String getApplicant();

    String getRespondent();

    String getPostCode();

    String getApplicationTitle();

    boolean isFeeRequired();

    String result();
}
