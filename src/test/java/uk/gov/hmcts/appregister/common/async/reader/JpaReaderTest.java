package uk.gov.hmcts.appregister.common.async.reader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;

@ExtendWith(MockitoExtension.class)
public class JpaReaderTest extends AbstractAsyncTest {

    @Captor ArgumentCaptor<Pageable> pageableArgumentCaptor;

    @Captor ArgumentCaptor<List<ApplicationCode>> applicationCodeArgumentCaptor;

    @Test
    void testRead() throws Exception {
        ReadPagePosition readPagePosition = new ReadPagePosition(1, 0);

        // setup the application code data to be returned by the supplier
        ApplicationCode code = new ApplicationCode();
        ApplicationCode code1 = new ApplicationCode();

        JobContext jobContext = mock(JobContext.class);

        // setup the page data callback
        PageReader<ApplicationCode> pageRead = Mockito.mock(PageReader.class);

        // gets the lust of paged data
        PageImpl<ApplicationCode> page = new PageImpl<>(List.of(code));
        PageImpl<ApplicationCode> page1 = new PageImpl<>(List.of(code1));
        PageImpl<ApplicationCode> page2 = new PageImpl<>(List.of());

        // setup the page data supplier callback
        Function<Pageable, Page<ApplicationCode>> supplier = mock(Function.class);

        // mock the supplier callback
        when(supplier.apply(pageableArgumentCaptor.capture())).thenReturn(page, page1, page2);

        // run the test
        try (JpaDataReader<ApplicationCode> applicationCodeJpaDataReader =
                new JpaDataReader<>(supplier)) {
            applicationCodeJpaDataReader.readData(readPagePosition, pageRead, jobContext);

            // Assert the pagination worked fine
            Assertions.assertEquals(
                    0, pageableArgumentCaptor.getAllValues().get(0).getPageNumber());
            Assertions.assertEquals(
                    1, pageableArgumentCaptor.getAllValues().get(1).getPageNumber());
            Assertions.assertEquals(
                    2, pageableArgumentCaptor.getAllValues().get(2).getPageNumber());

            // assert the number of times that the read callback was called
            Mockito.verify(pageRead, times(2))
                    .readData(applicationCodeArgumentCaptor.capture(), Mockito.eq(jobContext));

            // ensure each call to the read data passed a sub set of the data
            Assertions.assertEquals(1, applicationCodeArgumentCaptor.getAllValues().get(0).size());
            Assertions.assertEquals(
                    code, applicationCodeArgumentCaptor.getAllValues().get(0).get(0));

            Assertions.assertEquals(1, applicationCodeArgumentCaptor.getAllValues().get(1).size());
            Assertions.assertEquals(
                    code1, applicationCodeArgumentCaptor.getAllValues().get(1).get(0));
        }
    }
}
