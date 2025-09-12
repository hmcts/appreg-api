package uk.gov.hmcts.appregister.applicationentry.dto;

import java.util.List;

/**
 * DTO for moving applications to a different list.
 *
 * @param applicationIds List of application IDs to be moved.
 * @param targetListId ID of the target list where applications will be moved.
 */
public record ApplicationMoveRequestDto(List<Long> applicationIds, Long targetListId) {}
