package uk.gov.hmcts.appregister.common.async.reader;

import java.io.IOException;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.async.JobContext;

/**
 * A useful data reader that reads data from a Spring JPA repository.
 */
public class JpaDataReader<T> implements DataReader<T> {

    /** A function to get the relevant data on a per page basis. */
    private Function<Pageable, Page<T>> getEntityFunction;

    /**
     * Creates a new JpaDataReader.
     *
     * @param getEntityFunction A function that will return a page of data.
     */
    public JpaDataReader(Function<Pageable, Page<T>> getEntityFunction) {
        this.getEntityFunction = getEntityFunction;
    }

    @Override
    public void readData(
            ReadPagePosition position, PageReader<T> pageImporter, JobContext jobContext)
            throws IOException {
        // get the page of information
        Page<T> page = getEntityFunction.apply(convertToPageable(position));

        // loop through all pages
        while (!page.getContent().isEmpty()) {

            // pass the page of data to the page reader callback for processing
            pageImporter.readData(page.getContent(), jobContext);

            // now shift the position to get the next page
            position.setStartOffset(position.getStartOffset() + position.getPageSize());

            // get the next page of data
            page = getEntityFunction.apply(convertToPageable(position));
        }
    }

    /**
     * Converts the current page position to a Spring Pageable object.
     *
     * @param position The position to convert.
     * @return The converted Pageable object.
     */
    private Pageable convertToPageable(ReadPagePosition position) {
        return Pageable.ofSize(position.getPageSize())
                .withPage(position.getStartOffset() / position.getPageSize());
    }

    @Override
    public void close() throws IOException {
        // do nothing. No stream to close.
    }
}
