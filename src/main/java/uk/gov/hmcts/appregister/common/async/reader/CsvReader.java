package uk.gov.hmcts.appregister.common.async.reader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.exceptions.CsvException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

/**
 * A csv reader that reads pages of data from a generic csv file and pages the content according.
 * This class takes a copy of the csv file. As such, any use of this reader should use within a try
 * resources to ensure the file is deleted.
 */
@Getter
@Setter
@Slf4j
public class CsvReader<T extends CsvPojo> implements DataReader<T> {

    /** The file to read from. */
    private File source;

    /** The class to unmarshal into. */
    private Class<T> cls;

    /** The default csv delimiter to read. */
    private static final char DEFAULT_DELIMITER = '|';

    public CsvReader(MultipartFile file, Class<T> cls) throws IOException {
        this(file.getInputStream(), cls);
    }

    public CsvReader(File file, Class<T> cls) throws IOException {
        this(new FileInputStream(file), cls);
    }

    public CsvReader(InputStream is, Class<T> cls) throws IOException {
        source = AppRegTempFileUtil.generateTempFile("csv-reader");
        try (InputStream closeableis = is;
                FileOutputStream fileOutputStream = new FileOutputStream(source)) {
            closeableis.transferTo(fileOutputStream);
            this.cls = cls;
        }
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(source.toPath());
    }

    @Override
    public void readData(
            ReadPagePosition position, PageReader<T> keyableFunction, JobContext context)
            throws IOException {
        CsvToBean<T> csvToBean;
        // close the reader if it is open
        try (FileReader reader = new FileReader(source, guessCharset(source))) {
            csvToBean =
                    new CsvToBeanBuilder<T>(reader)
                            .withType(cls)
                            .withSeparator(DEFAULT_DELIMITER)
                            .build();
            ErrorToJobContext error = new ErrorToJobContext(context);
            csvToBean.setExceptionHandler(error);

            // skip the page if required
            Iterator<T> it = csvToBean.stream().iterator();

            // skip the page if required until we get to the offset size
            for (int i = 0; i < position.startOffset && it.hasNext(); i++) {
                it.next(); // skip without stream overhead
            }

            // now return the next page
            while (it.hasNext()) {
                List<T> listCsvData = new ArrayList<>();

                // loop around the page size for the next page
                for (int i = 0; i < position.getPageSize(); i++) {
                    // only load the next record if we can
                    if (it.hasNext()) {
                        listCsvData.add(it.next());
                    } else {
                        break;
                    }
                }

                if (!listCsvData.isEmpty()) {
                    // pass the read page to the reader
                    keyableFunction.readData(listCsvData, context);
                }
            }
        }
    }

    /**
     * gets the input stream that has built up each time a page is processed.
     *
     * @return The input stream.
     */
    @JsonIgnore
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(source);
    }

    /** Converts any csv exception to a job error and adds it to the job context for storage. */
    @RequiredArgsConstructor
    class ErrorToJobContext implements CsvExceptionHandler {
        private final JobContext jobContext;

        @Override
        public CsvException handleException(CsvException e) throws CsvException {
            log.error("Error reading csv file", e);
            // add the csv error to the job context
            if (jobContext != null) {
                jobContext.logFailure(e.getMessage());
                // ignore the exception from being thrown
                return null;
            } else {
                return e;
            }
        }
    }

    private Charset guessCharset(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()));
        if (content.contains("�")) {
            log.info("Detected Windows-1252 encoding for file: {}", file.getName());
            return Charset.forName("Windows-1252");
        } else {
            log.info("Unable to determine encoding using fallback UTF-8 encoding for file: {}", file.getName());
            return StandardCharsets.UTF_8;
        }
    }
}
