package uk.gov.hmcts.appregister.testutils.csv;

import java.io.IOException;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.lifecycle.AbstractCsvWriterAsyncLifecycle;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

/**
 * A test class to convert a jpa object {@link ApplicationCode} to a csv pojo {@link
 * ApplicationCodeCsvPojo} and write the data to a csv file. The csv file is applied to the job that
 * the events were source from.
 */
@Setter
public class JobProcessCsvWriteLifecycle
        extends AbstractCsvWriterAsyncLifecycle<ApplicationCode, ApplicationCodeCsvPojo> {

    public JobProcessCsvWriteLifecycle(CsvWriter<ApplicationCodeCsvPojo> csvWriter) {
        super(csvWriter);
    }

    private int pageProcessing = 0;

    @Override
    protected ApplicationCodeCsvPojo convert(ApplicationCode data) throws IOException {
        ApplicationCodeCsvPojo pojo = new ApplicationCodeCsvPojo();
        pojo.setCode(data.getCode());
        pojo.setTitle(data.getTitle());
        pojo.setWording(data.getWording());
        pojo.setFeedue(data.getFeeDue() == YesOrNo.YES);

        return pojo;
    }
}
