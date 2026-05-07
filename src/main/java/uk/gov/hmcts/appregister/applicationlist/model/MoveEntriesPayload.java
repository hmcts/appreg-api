package uk.gov.hmcts.appregister.applicationlist.model;

import java.util.UUID;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

/**
 * Validation payload for moving entries between application lists.
 */
public record MoveEntriesPayload(UUID sourceListId, MoveEntriesDto moveEntriesDto) {}
