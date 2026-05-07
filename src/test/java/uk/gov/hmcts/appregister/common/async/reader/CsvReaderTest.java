package uk.gov.hmcts.appregister.common.async.reader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.PersonCsvPojo;

/**
 * Tests the CSV reader.
 */
class CsvReaderTest extends AbstractAsyncTest {

    @Test
    void testRead() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("person.csv");
        File fileToLoad = new File(resource.getFile());

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
    public void testFailFormat() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("person_failformat.csv");
        File fileToLoad = new File(resource.getFile());

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
    }

    @Test
    public void testDataTypeError() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("person_faildataformat.csv");
        File fileToLoad = new File(resource.getFile());

        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);
        JobContext jobContext = new JobContext();
        List<PersonCsvPojo> output = new ArrayList<>();
        try (CsvReader<PersonCsvPojo> csvReader =
                new CsvReader<PersonCsvPojo>(fileToLoad, PersonCsvPojo.class)) {
            csvReader.readData(readPagePosition, (e, context) -> output.addAll(e), jobContext);
        }

        Assertions.assertEquals(
                "Conversion of notanumber to int failed.",
                jobContext.getCommaDelimitedFailureMessage());
    }
}
