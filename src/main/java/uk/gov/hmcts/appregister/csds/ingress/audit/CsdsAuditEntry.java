package uk.gov.hmcts.appregister.csds.ingress.audit;

public record CsdsAuditEntry(
        String appregTableName,
        String appregAction,
        Long appregKey,
        String csdsJson,
        String error) {}
