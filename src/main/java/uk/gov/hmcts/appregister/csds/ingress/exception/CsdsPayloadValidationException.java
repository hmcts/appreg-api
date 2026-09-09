package uk.gov.hmcts.appregister.csds.ingress.exception;

/**
 * Indicates that CSDS payload data does not satisfy the schema required by an ingress processor.
 */
public class CsdsPayloadValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CsdsPayloadValidationException(String message) {
        super(message);
    }
}
