package uk.gov.hmcts.appregister.applicationentry.dto;

public record BulkUploadErrorDto(int rowNumber, CsvRowDto row, String errorMessage) {}
