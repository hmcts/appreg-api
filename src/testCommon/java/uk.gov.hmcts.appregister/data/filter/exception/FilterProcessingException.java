package uk.gov.hmcts.appregister.data.filter.exception;

public class FilterProcessingException extends RuntimeException{
    public FilterProcessingException(Exception e) {
        super("Bad filter processing", e);
    }
}
