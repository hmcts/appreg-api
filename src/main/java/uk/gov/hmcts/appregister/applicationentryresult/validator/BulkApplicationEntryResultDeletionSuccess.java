package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class BulkApplicationEntryResultDeletionSuccess {
    private final List<BulkApplicationEntryResultDeletionValidatedItem> results = new ArrayList<>();
}
