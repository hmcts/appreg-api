package uk.gov.hmcts.appregister.common.async.reader;

import org.springframework.data.domain.Page;

import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;

public class JpaDataReader<T> implements DataReader<T>  {
    private static int LIMIT = 100;

    @Override
    public void readData(JobTypeRequest request, Class<T> cls, PageRead<T> pageImporter) {
        ReadPagePosition readPagePosition = null;

        Page<T> page =  getNextPageOfData(readPagePosition == null
                                      ? readPagePosition = new ReadPagePosition(LIMIT) : readPagePosition);

        for (int i =0; i < page.getTotalPages(); i++) {
            readPagePosition.setOffset(i);
            pageImporter.readData(readPagePosition, page.getContent());
        }
    }

    /**
     * gets the next page of data
     * @return The list of data
     */
    protected abstract Page<T> getNextPageOfData(ReadPagePosition page);
}

