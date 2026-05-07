package uk.gov.hmcts.appregister.common.async.writer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.PersonCsvPojo;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

public class CsvWriterTest extends AbstractAsyncTest {

    @AfterEach
    void tearDown() {
        // ensure that we do not leave any temp files around.
        if (AppRegTempFileUtil.doesTempFileExist()) {
            // mark for deletion when the process ends
            Arrays.asList(AppRegTempFileUtil.getTempFilesThatExist()).forEach(File::deleteOnExit);

            throw new AssertionFailure(
                    "You're code is not clearing up temp files that it creates, please make sure "
                            + "you delete files by wrapping code in try/resources where necessary.");
        }
    }

    @Test
    void testWriteAppendsReadData() throws IOException {
        try (CsvWriter<PersonCsvPojo> writer = new CsvWriter<>(PersonCsvPojo.class)) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource("person.csv");
            File fileToLoad = new File(resource.getFile());

            ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);

            try (CsvReader<PersonCsvPojo> csvReader =
                    new CsvReader<PersonCsvPojo>(fileToLoad, PersonCsvPojo.class)) {
                csvReader.readData(readPagePosition, writer::write, null);
            }

            List<PersonCsvPojo> output = new ArrayList<>();

            readPagePosition = new ReadPagePosition(1, 0);
            try (CsvReader<PersonCsvPojo> reader =
                    new CsvReader(writer.getInputStream(), PersonCsvPojo.class)) {
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
