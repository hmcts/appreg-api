package uk.gov.hmcts.appregister.common.async;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;

/**
 * An open csv annotated POJO for test purposes.
 */
@Getter
@Setter
public class PersonCsvPojo implements CsvPojo {
    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "age")
    private int age;

    @CsvBindByName(column = "email")
    private String email;
}
