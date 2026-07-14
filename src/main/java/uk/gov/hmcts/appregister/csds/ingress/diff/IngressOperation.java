package uk.gov.hmcts.appregister.csds.ingress.diff;

/**
 * Supported outcomes when comparing incoming CSDS data with the local representation.
 */
public enum IngressOperation {
    INSERT,
    UPDATE
}
