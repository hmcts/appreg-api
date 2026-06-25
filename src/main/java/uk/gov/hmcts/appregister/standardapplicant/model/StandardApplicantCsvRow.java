package uk.gov.hmcts.appregister.standardapplicant.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;

@Getter
@Setter
public class StandardApplicantCsvRow implements CsvPojo {

    public static final String[] Header = {
        "Applicant Code",
        "Applicant Title",
        "Name",
        "Forename 1",
        "Forename 2",
        "Forename 3",
        "Surname",
        "Address Line 1",
        "Address Line 2",
        "Address Line 3",
        "Address Line 4",
        "Address Line 5",
        "Postcode",
        "Email Address",
        "Telephone Number",
        "Mobile Number",
        "Use From",
        "Use To"
    };

    @CsvBindByPosition(position = 0)
    private String applicantCode;

    @CsvBindByPosition(position = 1)
    private String applicantTitle;

    @CsvBindByPosition(position = 2)
    private String name;

    @CsvBindByPosition(position = 3)
    private String applicantForename1;

    @CsvBindByPosition(position = 4)
    private String applicantForename2;

    @CsvBindByPosition(position = 5)
    private String applicantForename3;

    @CsvBindByPosition(position = 6)
    private String applicantSurname;

    @CsvBindByPosition(position = 7)
    private String addressLine1;

    @CsvBindByPosition(position = 8)
    private String addressLine2;

    @CsvBindByPosition(position = 9)
    private String addressLine3;

    @CsvBindByPosition(position = 10)
    private String addressLine4;

    @CsvBindByPosition(position = 11)
    private String addressLine5;

    @CsvBindByPosition(position = 12)
    private String postcode;

    @CsvBindByPosition(position = 13)
    private String emailAddress;

    @CsvBindByPosition(position = 14)
    private String telephoneNumber;

    @CsvBindByPosition(position = 15)
    private String mobileNumber;

    @CsvBindByPosition(position = 16)
    private String applicantStartDate;

    @CsvBindByPosition(position = 17)
    private String applicantEndDate;
}
