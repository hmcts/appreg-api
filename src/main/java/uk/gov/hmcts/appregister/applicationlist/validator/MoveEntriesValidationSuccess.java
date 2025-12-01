package uk.gov.hmcts.appregister.applicationlist.validator;

import lombok.Data;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

@Data
public class MoveEntriesValidationSuccess {
    private ApplicationList targetList;
}
