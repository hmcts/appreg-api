package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.List;
import lombok.Data;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

@Data
public class BulkUpdateFeesValidationSuccess {
    private List<ApplicationListEntry> entries;
}
