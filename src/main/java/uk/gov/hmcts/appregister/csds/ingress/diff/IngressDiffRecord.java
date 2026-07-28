package uk.gov.hmcts.appregister.csds.ingress.diff;

/**
 * One diff decision for a single incoming record.
 */
public record IngressDiffRecord<I, E, N>(
        IngressOperation operation, I incoming, E existing, N intended, String reason) {}
