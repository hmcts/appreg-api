package uk.gov.hmcts.appregister.csds.ingress.database;

public record FailedUpsertRecord<T>(T item, String errorMessage) {}
