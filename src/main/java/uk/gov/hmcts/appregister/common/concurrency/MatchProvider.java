package uk.gov.hmcts.appregister.common.concurrency;

/**
 * An interface that should understand how to return an etag for the current request.
 */
public interface MatchProvider {
    /** Get the ETag value for concurrency control. */
    String getEtag();
}
