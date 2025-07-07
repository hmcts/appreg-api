package uk.gov.hmcts.appregister.dto.write;

import java.util.List;

public record ApplicationMoveRequestDto(
    List<Long> applicationIds,
    Long targetListId
) {}
