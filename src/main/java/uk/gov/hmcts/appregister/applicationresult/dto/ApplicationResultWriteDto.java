package uk.gov.hmcts.appregister.applicationresult.dto;

import java.util.List;

/** DTO for writing application result data. */
public record ApplicationResultWriteDto(
        Long resultCodeId, List<String> textFields, String resultOfficer) {}
