package uk.gov.hmcts.appregister.csds.ingress.diff;

/**
 * One diff decision for a single incoming record.
 */
public record IngressDiffRecord<TIncoming, TExisting, TIntended>(
        IngressOperation operation,
        TIncoming incoming,
        TExisting existing,
        TIntended intended,
        String reason) {}
