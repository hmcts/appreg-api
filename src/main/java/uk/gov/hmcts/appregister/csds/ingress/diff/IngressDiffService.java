package uk.gov.hmcts.appregister.csds.ingress.diff;

/**
 * Calculates ingress actions from a processor-specific diff request.
 */
public interface IngressDiffService<R, D> {
    D diff(R request);
}
