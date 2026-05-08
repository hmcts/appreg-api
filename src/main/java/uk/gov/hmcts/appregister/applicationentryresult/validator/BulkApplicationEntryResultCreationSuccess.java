package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

@Getter
@Setter
public class BulkApplicationEntryResultCreationSuccess {
    final List<PayloadForCreateEntryResult<ResultCreateDto>> results = new ArrayList<>();
}
