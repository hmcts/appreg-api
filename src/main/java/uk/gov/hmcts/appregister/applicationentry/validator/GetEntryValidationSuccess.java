package uk.gov.hmcts.appregister.applicationentry.validator;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

@Builder
@Getter
@Setter
public class GetEntryValidationSuccess {
    private final ApplicationListEntry applicationListEntry;
    private final ApplicationList applicationList;

}
