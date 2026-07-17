package uk.gov.hmcts.appregister.common.async.writer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.PersonCsvPojo;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;

class CsvWriterTest extends AbstractAsyncTest {

    @Test
    void testWriteAppendsReadData() throws IOException {
        try (CsvWriter<PersonCsvPojo> writer = new CsvWriter<>(PersonCsvPojo.class)) {
            File fileToLoad = testResourceFile("person.csv");

            ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);

            try (CsvReader<PersonCsvPojo> csvReader =
                    new CsvReader<PersonCsvPojo>(fileToLoad, PersonCsvPojo.class)) {
                csvReader.readData(readPagePosition, writer::write, null);
            }

            List<PersonCsvPojo> output = new ArrayList<>();

            readPagePosition = new ReadPagePosition(1, 0);
            try (CsvReader<PersonCsvPojo> reader =
                    new CsvReader<>(writer.getInputStream(), PersonCsvPojo.class)) {
                reader.readData(readPagePosition, (e, context) -> output.addAll(e), null);

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
        }
    }
}
