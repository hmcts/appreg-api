package uk.gov.hmcts.appregister.action.validator;

import java.util.List;
import lombok.Data;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

@Data
public class MoveEntriesValidationSuccess {
    private ApplicationList sourceList;
    private ApplicationList targetList;
    private List<ApplicationListEntry> entriesToSave;
}
