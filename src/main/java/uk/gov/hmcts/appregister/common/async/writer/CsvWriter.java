package uk.gov.hmcts.appregister.common.async.writer;

import com.opencsv.bean.StatefulBeanToCsv;

import com.opencsv.bean.StatefulBeanToCsvBuilder;

import com.opencsv.exceptions.CsvException;

import lombok.extern.slf4j.Slf4j;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import uk.gov.hmcts.appregister.common.async.CsvPojo;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
public class CsvWriter<T extends CsvPojo> implements PageWrite<T> {
    private File file;

    private FileWriter writer;

    private CsvWriter() throws IOException {
        this.file = new File(UUID.randomUUID().toString());
    }

    @Override
    public void close() throws IOException {
        file.delete();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.copy(new FileInputStream(file), new FileOutputStream(file));
    }

    /**
     * writes the csv to the file.
     * @param csv The records to write
     * @return The true or false if the write was successful.
     */
    public boolean write(List<T> csv) {
        try (FileWriter writer = new FileWriter(file)) {
                StatefulBeanToCsv<T> beanToCsv =
                    new StatefulBeanToCsvBuilder<T>(writer)
                        .withApplyQuotesToAll(false)
                        .build();

                beanToCsv.write(csv); // must pass a collection
        } catch (IOException e) {
            log.error("Error writing csv file", e);
            throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER, "Error writing csv file", e);
        }
        catch (CsvException e) {
            log.error("Error writing csv file", e);
            throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER, "Error writing csv file", e);
        }
    }
}
