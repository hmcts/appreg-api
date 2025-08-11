package uk.gov.hmcts.appregister.applicationentry.dto;

import java.util.List;

public record BulkUploadResponseDto(int successfulCount, List<BulkUploadErrorDto> errors) {}
