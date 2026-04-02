package uk.gov.hmcts.appregister.common.async.reader;

import java.util.List;

/**
 * Represents a
 */
@FunctionalInterface
public interface PageRead<T> extends Cloneable {
    /**
     * imports the page data from the csv file and converts it to a list of objects.
     * @param position The position of the data to import.
     * @param relatedData The related data to import.
     */
    void readData(ReadPagePosition position, List<T> relatedData);
}
