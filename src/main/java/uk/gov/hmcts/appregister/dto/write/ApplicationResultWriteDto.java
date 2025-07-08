package uk.gov.hmcts.appregister.dto.write;

import java.util.List;

public record ApplicationResultWriteDto(
        Long resultCodeId, List<String> textFields, String resultOfficer) {}
