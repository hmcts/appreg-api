package uk.gov.hmcts.appregister.data.filter.exception;

/**
 * A filter processing exception. This is thrown when there is a problem processing using the filter
 * test.
 */
public class FilterProcessingException extends RuntimeException {
    public FilterProcessingException(Exception e) {
        super("Bad filter processing", e);
    }

    public FilterProcessingException(String msg) {
        super(msg);
    }
}
