package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkApplicationEntryResultCreationSuccess {
    final List<BulkApplicationEntryResultValidatedItem> results = new ArrayList<>();
}
