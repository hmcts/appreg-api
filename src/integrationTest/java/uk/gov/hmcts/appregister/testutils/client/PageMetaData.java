package uk.gov.hmcts.appregister.testutils.client;

/** Paging query meta data. Allows tests to use different query parameter names for paging. */
public interface PageMetaData {

    /**
     * Get the query parameter name for page number.
     *
     * @return the query parameter name for page number
     */
    String getPageNumberQueryName();

    /**
     * Get the query parameter name for page size.
     *
     * @return the query parameter name for page size
     */
    String getPageSizeQueryName();

    /**
     * Get the query parameter name for sort.
     *
     * @return the query parameter name for sort
     */
    String getSortName();
}
