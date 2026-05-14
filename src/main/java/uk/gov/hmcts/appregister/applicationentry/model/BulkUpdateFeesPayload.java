package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.UUID;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;

public record BulkUpdateFeesPayload(UUID listId, BulkFeesUpdateDto data) {}
