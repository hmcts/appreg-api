package uk.gov.hmcts.appregister.testutils.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;

/**
 * An application code test pojo.
 */
@Getter
@Setter
@EqualsAndHashCode
public class ApplicationCodeCsvPojo implements CsvPojo {
    @CsvBindByName(column = "code")
    private String code;

    @CsvBindByName(column = "title")
    private String title;

    @CsvBindByName(column = "wording")
    private String wording;

    @CsvBindByName(column = "feedue")
    private Boolean feedue;
}
