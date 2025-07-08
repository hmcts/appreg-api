package uk.gov.hmcts.appregister.dto.read.bulkupload;

import java.util.List;

public record BulkUploadResponseDto(int successfulCount, List<BulkUploadErrorDto> errors) {}
