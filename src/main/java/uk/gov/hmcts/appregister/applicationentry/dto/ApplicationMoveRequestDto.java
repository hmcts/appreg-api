package uk.gov.hmcts.appregister.applicationentry.dto;

import java.util.List;

public record ApplicationMoveRequestDto(List<Long> applicationIds, Long targetListId) {}
