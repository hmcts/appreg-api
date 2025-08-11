package uk.gov.hmcts.appregister.applicationresult.dto;

import java.util.List;

public record ApplicationResultWriteDto(
        Long resultCodeId, List<String> textFields, String resultOfficer) {}
