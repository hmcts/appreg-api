package uk.gov.hmcts.appregister.dto.read.bulkupload;

public record BulkUploadErrorDto (
    int rowNumber,
    CsvRowDto row,
    String errorMessage
){}
