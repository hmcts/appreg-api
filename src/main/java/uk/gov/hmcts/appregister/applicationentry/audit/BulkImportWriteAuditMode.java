package uk.gov.hmcts.appregister.applicationentry.audit;

/**
 * Controls whether bulk imports are audited once per job or once per imported entry.
 */
public enum BulkImportWriteAuditMode {
    BULK,
    PER_ENTRY
}
