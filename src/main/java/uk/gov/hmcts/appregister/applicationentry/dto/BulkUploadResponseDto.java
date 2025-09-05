package uk.gov.hmcts.appregister.applicationentry.dto;

import java.util.List;

/** DTO representing the response from a bulk upload operation. */
public record BulkUploadResponseDto(int successfulCount, List<BulkUploadErrorDto> errors) {}
