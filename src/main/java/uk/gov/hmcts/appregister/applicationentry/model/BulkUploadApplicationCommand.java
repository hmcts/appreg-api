package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command object representing one row from the bulk upload CSV.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadApplicationCommand {

    private String applicantCode;

    private String respondentTitle;
    private String respondentOrganisationName;

    private String respondentForename1;
    private String respondentForename2;
    private String respondentForename3;
    private String respondentSurname;

    private String respondentAddress1;
    private String respondentAddress2;
    private String respondentAddress3;
    private String respondentAddress4;
    private String respondentAddress5;

    private String respondentPostcode;
    private String respondentEmail;
    private String respondentTelephone;
    private String respondentMobile;

    private String accountNumber;

    private String applicationCode;

    @Builder.Default
    private List<String> applicationTexts = new ArrayList<>();
}
