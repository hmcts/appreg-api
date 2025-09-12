package uk.gov.hmcts.appregister.applicationlistentryresolutions.dto;

import java.util.List;

/** DTO for writing application result data. */
public record ApplicationListEntryResolutionWriteDto(
        Long resultCodeId, List<String> textFields, String resultOfficer) {}
