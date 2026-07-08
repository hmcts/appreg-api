package uk.gov.hmcts.appregister.standardapplicant.model;

import com.opencsv.bean.CsvBindByPosition;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;

@Getter
@Setter
public class StandardApplicantCsvRow implements CsvPojo {

    public static final List<String> Header =
            List.of("Applicant Code", "Name", "Use From", "Use To");

    @CsvBindByPosition(position = 0)
    private String applicantCode;

    @CsvBindByPosition(position = 1)
    private String name;

    @CsvBindByPosition(position = 2)
    private String applicantStartDate;

    @CsvBindByPosition(position = 3)
    private String applicantEndDate;
}
