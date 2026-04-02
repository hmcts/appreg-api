package uk.gov.hmcts.appregister.common.async.reader;

import com.opencsv.bean.CsvToBean;

import com.opencsv.bean.CsvToBeanBuilder;

import lombok.Getter;
import lombok.Setter;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import org.springframework.web.multipart.MultipartFile;

import uk.gov.hmcts.appregister.common.async.CsvPojo;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.UUID;

/**
 * A csv importer that reads from a generic csv file and pages accordingly.
 */
@Getter
@Setter
public class CsvReader<T extends CsvPojo> implements DataReader<T> {

    /** The limit of records to read from the csv. */
    private static final int DEFAULT_SIZE_LIMIT = 100;

    /** The position of the offset in the csv. */
    private int offsetPosition = 0;

    /** The size of the records to load into memory */
    private int loadSize = DEFAULT_SIZE_LIMIT;

    private File source;

    public CsvReader(MultipartFile file) throws IOException {
        source = new File(UUID.randomUUID().toString());
        FileOutputStream fileOutputStream = new FileOutputStream(source);
        IOUtils.copy(file.getInputStream(), fileOutputStream);
    }

    @Override
    public void close() throws IOException {
        source.delete();
    }

    @Override
    public <T> void importData(Class<T> cls, PageRead<T> keyableFunction) {
        ReadPagePosition importPagePosition = new ReadPagePosition();
        importPagePosition.offset = offsetPosition;
        importPagePosition.limit = loadSize;

        try {
            List<T> listCsvData = readCsv(new FileReader(source), cls, 0, loadSize);

            // loop around all of the pages in the csv file
            while (listCsvData != null) {
                if (!keyableFunction.importData(importPagePosition, listCsvData)) {
                    listCsvData = readCsv(source, cls, loadSize, offsetPosition);
                } else {
                    throw new AppRegistryException(
                        JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER,
                        "Problems importing the csv data"
                    );
                }
            }
        } catch (FileNotFoundException fileNotFoundException) {
            throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER,
                                           "File not found", fileNotFoundException);
        }
    }

    /**
     * reads the csv file and returns the records.
     * @return The type of the records.
     */
    private <T> List<T> readCsv(Reader source, Class<T> clzz, int limit, int offsetPosition) {
        try (Reader reader = source) {
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                .withType(clzz)
                .withSkipLines(1) // header
                .build();

            return csvToBean.stream()
                .skip(offsetPosition)
                .limit(limit)
                .toList();
        } catch (IOException ioException) {
            throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER,
                                           "Error reading csv file", ioException);
        }
    }
}
