package uk.gov.hmcts.appregister.common.async.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.HeaderNameBaseMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

/**
 * A open csv writer that writes a set of {@link CsvPojo} to a generic csv file with multiple
 * results. This writer is appender and write can be called multiple times. On close of this writer
 * the file is deleted. Use of this resource should ensure its use within a try resources context
 */
@Slf4j
public class CsvWriter<T extends CsvPojo> implements PageWriter<T> {

    /** The representation of the underlying file. */
    private File file;

    /**
     * The default delimeter for csv is pipes not commas. This aligns ourself with the expected
     * delimiter
     */
    private static final char DEFAULT_DELIMITER = '|';

    /**
     * A no header strategy that allows us to append to the underlying file whilst ensuring we have
     * one set of headers.
     */
    private NoHeaderStrategy<T> noHeaderStrategy;

    public CsvWriter(Class<T> cls) throws IOException {
        this.file = AppRegTempFileUtil.generateTempFile("csv-writer");
        noHeaderStrategy = new NoHeaderStrategy<>(file);
        noHeaderStrategy.setType(cls);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(file.toPath());
    }

    @Override
    @JsonIgnore
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    /**
     * writes the csv to the file.
     *
     * @param csv The records to write
     * @param jobContext The context to log any errors.
     * @throws IOException Any problems writing the csv.
     */
    public void write(List<T> csv, JobContext jobContext) throws IOException {
        try (FileWriter writer = new FileWriter(file, true)) {

            // open the csv writer
            StatefulBeanToCsv<T> beanToCsv =
                    new StatefulBeanToCsvBuilder<T>(writer)
                            .withApplyQuotesToAll(false)
                            .withSeparator(DEFAULT_DELIMITER)
                            .withMappingStrategy(noHeaderStrategy)
                            .build();

            // any specific errors related to formatting are logged to the context by default
            try {

                // write the csv pojos to a csv file
                beanToCsv.write(csv); // must pass a collection
            } catch (CsvDataTypeMismatchException dataTypeMismatchException) {
                jobContext.logFailure(dataTypeMismatchException.getMessage());
            } catch (CsvRequiredFieldEmptyException csvRequiredFieldEmptyException) {
                jobContext.logFailure(csvRequiredFieldEmptyException.getMessage());
            }
        }
    }

    /** Do not add a header if it has already been written. */
    public class NoHeaderStrategy<R> extends HeaderNameBaseMappingStrategy<R> {

        private final File file;

        public NoHeaderStrategy(File file) {
            this.file = file;
        }

        @Override
        public String[] generateHeader(R bean) throws CsvRequiredFieldEmptyException {
            // if the file is empty, then we can write the header, otherwise
            // do not bother
            if (file.length() != 0) {
                return new String[0]; // <-- prevents header from being written
            }

            // generate the header
            return super.generateHeader(bean);
        }
    }
}
