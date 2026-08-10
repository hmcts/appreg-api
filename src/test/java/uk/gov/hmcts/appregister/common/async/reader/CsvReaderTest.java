package uk.gov.hmcts.appregister.common.async.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.PersonCsvPojo;

/**
 * Tests the CSV reader.
 */
class CsvReaderTest extends AbstractAsyncTest {

    @Test
    void testRead() throws IOException {
        File fileToLoad = testResourceFile("person.csv");

        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);

        List<PersonCsvPojo> output = new ArrayList<>();
        try (CsvReader<PersonCsvPojo> csvReader =
                new CsvReader<PersonCsvPojo>(fileToLoad, PersonCsvPojo.class)) {
            csvReader.readData(readPagePosition, (e, context) -> output.addAll(e), null);
        }

        Assertions.assertEquals(3, output.size());
        Assertions.assertEquals("Alice", output.get(0).getName());
        Assertions.assertEquals(30, output.get(0).getAge());
        Assertions.assertEquals("alice@example.com", output.get(0).getEmail());

        Assertions.assertEquals("Bob", output.get(1).getName());
        Assertions.assertEquals(25, output.get(1).getAge());
        Assertions.assertEquals("bob@example.com", output.get(1).getEmail());

        Assertions.assertEquals("Carwyn", output.get(2).getName());
        Assertions.assertEquals(40, output.get(2).getAge());
        Assertions.assertEquals("car@example.com", output.get(2).getEmail());
    }

    @Test
    void testFailFormat() throws IOException {
        File fileToLoad = testResourceFile("person_failformat.csv");

        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);

        List<PersonCsvPojo> output = new ArrayList<>();
        JobContext jobContext = new JobContext();
        try (CsvReader<PersonCsvPojo> csvReader =
                new CsvReader<PersonCsvPojo>(fileToLoad, PersonCsvPojo.class)) {
            csvReader.readData(readPagePosition, (e, context) -> output.addAll(e), jobContext);
        }

        Assertions.assertEquals(
                "Number of data fields does not match number of headers.",
                jobContext.getCommaDelimitedFailureMessage());
        Assertions.assertTrue(jobContext.isFieldCountMismatch());
    }

    @Test
    void testReadWindows1252Format() throws IOException {
        File csv = testResourceFile("windows-1252.csv");

        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);
        JobContext context = new JobContext();
        List<BulkUploadRow> bulkUploadRowList = new ArrayList<>();
        try (CsvReader<BulkUploadRow> csvReader = new CsvReader<>(csv, BulkUploadRow.class)) {
            csvReader.readData(
                    readPagePosition, (e, jobContext) -> bulkUploadRowList.addAll(e), context);
        }

        Assertions.assertEquals(
                "£500.00", bulkUploadRowList.getFirst().getApplicationTextValues().getFirst());
    }

    @Test
    void testReadUTF8Format() throws IOException {
        File csv = testResourceFile("utf-8.csv");

        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);
        JobContext context = new JobContext();
        List<BulkUploadRow> bulkUploadRowList = new ArrayList<>();
        try (CsvReader<BulkUploadRow> csvReader = new CsvReader<>(csv, BulkUploadRow.class)) {
            csvReader.readData(
                    readPagePosition, (e, jobContext) -> bulkUploadRowList.addAll(e), context);
        }

        Assertions.assertEquals(
                "£500.00", bulkUploadRowList.getFirst().getApplicationTextValues().getFirst());
    }

    @Test
    void testDataTypeError() throws IOException {
        File fileToLoad = testResourceFile("person_faildataformat.csv");

        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);
        JobContext jobContext = new JobContext();
        List<PersonCsvPojo> output = new ArrayList<>();
        try (CsvReader<PersonCsvPojo> csvReader =
                new CsvReader<PersonCsvPojo>(fileToLoad, PersonCsvPojo.class)) {
            csvReader.readData(readPagePosition, (e, context) -> output.addAll(e), jobContext);
        }

        Assertions.assertEquals(
                "Uploaded file could not be parsed.", jobContext.getCommaDelimitedFailureMessage());
    }
}
