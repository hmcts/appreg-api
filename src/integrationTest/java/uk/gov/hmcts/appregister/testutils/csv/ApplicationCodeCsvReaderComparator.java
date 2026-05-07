package uk.gov.hmcts.appregister.testutils.csv;

import java.io.IOException;
import java.util.Comparator;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;

/**
 * Ensures that two readers contain the same results.
 */
public class ApplicationCodeCsvReaderComparator
        implements Comparator<CsvReader<ApplicationCodeCsvPojo>> {

    @Override
    public int compare(CsvReader<ApplicationCodeCsvPojo> o1, CsvReader<ApplicationCodeCsvPojo> o2) {
        ReadPagePosition position = new ReadPagePosition(0, 10);

        JobContext jobContext = new JobContext();
        try {
            try (CsvReader<ApplicationCodeCsvPojo> from = o1;
                    CsvReader<ApplicationCodeCsvPojo> to = o2) {

                from.readData(
                        position,
                        (pageOfFromData, context) -> {
                            to.readData(
                                    position,
                                    (pageOfToData, toContext) -> {
                                        if (!pageOfFromData.equals(pageOfToData)) {
                                            context.logFailure("Data not equal");
                                        }
                                    },
                                    jobContext);
                        },
                        jobContext);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jobContext.hasFailure() ? 1 : 0;
    }
}
