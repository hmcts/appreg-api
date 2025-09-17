package uk.gov.hmcts.appregister.applicationentry.dto;

/** DTO representing an error encountered during bulk upload of application entries. */
public record BulkUploadErrorDto(int rowNumber, CsvRowDto row, String errorMessage) {}
