package uk.gov.hmcts.appregister.common.async.lifecycle;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.PersonCsvPojo;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

class AbstractCsvWriterAsyncLifecycleTest {

    @Test
    void testAbstractCsvAsyncLifecycle() throws IOException {
        JobStatusResponse jobStatusResponse = Mockito.mock(JobStatusResponse.class);
        when(jobStatusResponse.getStatus()).thenReturn(JobStatus.FAILED);
        when(jobStatusResponse.getUserName()).thenReturn("user");
        when(jobStatusResponse.getErrorMessage()).thenReturn("error");

        NameAddress nameAddress = new NameAddress();
        nameAddress.setName("title");
        nameAddress.setEmailAddress("email");

        NameAddress nameAddress1 = new NameAddress();
        nameAddress1.setName("title");
        nameAddress1.setEmailAddress("email");

        DummyCsvAsyncLifecycle lifecycleUnderTest;

        JobContext jobContext = new JobContext();

        final CsvWriter<PersonCsvPojo> csvWriter = new CsvWriter<>(PersonCsvPojo.class);
        lifecycleUnderTest = new DummyCsvAsyncLifecycle(csvWriter);

        // first processing call
        lifecycleUnderTest.lifeCycleEventPerformed(
                new AsyncJobLifecycleEvent<>(
                        jobStatusResponse, List.of(nameAddress), jobContext, JobStatus.PROCESSING));

        // second processing call
        lifecycleUnderTest.lifeCycleEventPerformed(
                new AsyncJobLifecycleEvent<>(
                        jobStatusResponse,
                        List.of(nameAddress1),
                        jobContext,
                        JobStatus.PROCESSING));

        CsvReader<PersonCsvPojo> csvReader;
        csvReader = new CsvReader<>(csvWriter.getInputStream(), PersonCsvPojo.class);
        // now lets read the data
        try (csvReader) {
            List<PersonCsvPojo> output = new ArrayList<>();
            csvReader.readData(new ReadPagePosition(10, 0), (e, context) -> output.addAll(e), null);

            Assertions.assertEquals(2, output.size());
            Assertions.assertEquals(nameAddress.getName(), output.get(0).getName());
            Assertions.assertEquals(23, output.get(0).getAge());
            Assertions.assertEquals(nameAddress1.getEmailAddress(), output.get(0).getEmail());

            Assertions.assertEquals(nameAddress1.getName(), output.get(1).getName());
            Assertions.assertEquals(23, output.get(1).getAge());
            Assertions.assertEquals(nameAddress1.getEmailAddress(), output.get(1).getEmail());
        }

        // completed call
        lifecycleUnderTest.lifeCycleEventPerformed(
                new AsyncJobLifecycleEvent<>(
                        jobStatusResponse,
                        List.of(nameAddress, nameAddress1),
                        jobContext,
                        JobStatus.COMPLETED));

        // ensure we try to write the clob
        verify(jobStatusResponse).write(notNull());

        // make sure the reader and writer streams are closed
        Assertions.assertThrows(IOException.class, csvWriter::getInputStream);
        Assertions.assertThrows(IOException.class, csvReader::getInputStream);
    }

    @Test
    void testAbstractCsvAsyncLifecycleFail() throws IOException {
        JobStatusResponse jobStatusResponse = Mockito.mock(JobStatusResponse.class);
        when(jobStatusResponse.getStatus()).thenReturn(JobStatus.FAILED);
        when(jobStatusResponse.getUserName()).thenReturn("user");
        when(jobStatusResponse.getErrorMessage()).thenReturn("error");

        NameAddress nameAddress = new NameAddress();
        nameAddress.setName("title");
        nameAddress.setEmailAddress("email");

        NameAddress nameAddress1 = new NameAddress();
        nameAddress1.setName("title");
        nameAddress1.setEmailAddress("email");

        DummyCsvAsyncLifecycle lifecycleUnderTest;

        final CsvWriter<PersonCsvPojo> csvWriter = new CsvWriter<>(PersonCsvPojo.class);
        lifecycleUnderTest = new DummyCsvAsyncLifecycle(csvWriter);
        JobContext jobContext = new JobContext();

        // first processing call
        lifecycleUnderTest.lifeCycleEventPerformed(
                new AsyncJobLifecycleEvent<>(
                        jobStatusResponse, List.of(nameAddress), jobContext, JobStatus.PROCESSING));

        // second processing call
        lifecycleUnderTest.lifeCycleEventPerformed(
                new AsyncJobLifecycleEvent<>(
                        jobStatusResponse,
                        List.of(nameAddress1),
                        jobContext,
                        JobStatus.PROCESSING));

        CsvReader<PersonCsvPojo> csvReader;
        csvReader = new CsvReader<>(csvWriter.getInputStream(), PersonCsvPojo.class);
        // now lets read the data
        try (csvReader) {
            List<PersonCsvPojo> output = new ArrayList<>();
            csvReader.readData(new ReadPagePosition(10, 0), (e, context) -> output.addAll(e), null);

            Assertions.assertEquals(2, output.size());
            Assertions.assertEquals(nameAddress.getName(), output.get(0).getName());
            Assertions.assertEquals(23, output.get(0).getAge());
            Assertions.assertEquals(nameAddress1.getEmailAddress(), output.get(0).getEmail());

            Assertions.assertEquals(nameAddress1.getName(), output.get(1).getName());
            Assertions.assertEquals(23, output.get(1).getAge());
            Assertions.assertEquals(nameAddress1.getEmailAddress(), output.get(1).getEmail());
        }

        // fail processing
        lifecycleUnderTest.lifeCycleEventPerformed(
                new AsyncJobLifecycleEvent<>(
                        jobStatusResponse,
                        List.of(nameAddress, nameAddress1),
                        jobContext,
                        JobStatus.FAILED));

        // ensure we did not write the clob
        verify(jobStatusResponse, never()).write(notNull());

        // make sure the reader and writer streams are closed
        Assertions.assertThrows(IOException.class, csvWriter::getInputStream);
        Assertions.assertThrows(IOException.class, csvReader::getInputStream);
    }

    class DummyCsvAsyncLifecycle
            extends AbstractCsvWriterAsyncLifecycle<NameAddress, PersonCsvPojo> {
        public DummyCsvAsyncLifecycle(CsvWriter<PersonCsvPojo> csvWriter) {
            super(csvWriter);
        }

        @Override
        protected PersonCsvPojo convert(NameAddress data) throws IOException {
            PersonCsvPojo personData = new PersonCsvPojo();
            personData.setName(data.getName());
            personData.setAge(23);
            personData.setEmail(data.getEmailAddress());
            return personData;
        }
    }
}
