package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.UUID;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;

public record BulkUpdateOfficialsPayload(UUID listId, BulkOfficialsUpdateDto data) {}
