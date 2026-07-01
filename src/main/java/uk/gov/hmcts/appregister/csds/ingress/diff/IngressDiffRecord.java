package uk.gov.hmcts.appregister.csds.ingress.diff;

public record IngressDiffRecord<TIncoming, TExisting>(
        IngressOperation operation, TIncoming incoming, TExisting existing, String reason) {}
