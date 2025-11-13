package uk.gov.hmcts.appregister.common.projection;

public interface ApplicationListOfficialPrintProjection {

    Long getEntryId();

    String getType();

    String getTitle();

    String getForename();

    String getSurname();
}
