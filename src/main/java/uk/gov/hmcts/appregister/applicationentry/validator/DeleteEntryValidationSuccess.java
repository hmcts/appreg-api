package uk.gov.hmcts.appregister.applicationentry.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

@Getter
@RequiredArgsConstructor
public class DeleteEntryValidationSuccess {
    private final ApplicationListEntry applicationListEntry;
}
