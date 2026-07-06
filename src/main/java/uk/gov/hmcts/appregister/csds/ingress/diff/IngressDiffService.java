package uk.gov.hmcts.appregister.csds.ingress.diff;

/**
 * Calculates ingress actions from a processor-specific diff request.
 */
public interface IngressDiffService<RequestT, DiffT> {
    DiffT diff(RequestT request);
}
