package uk.gov.hmcts.appregister.util;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;

@Getter
@RequiredArgsConstructor
@lombok.Builder
public final class ApplicationListEntrySummaryProjectionBuilder
        implements ApplicationListEntrySummaryProjection {
    private final Long id;
    private final UUID uuid;
    private final short sequenceNumber;
    private final String accountNumber;
    private final NameAddress applicant;
    private final NameAddress respondent;
    private final StandardApplicant standardApplicant;
    private final String postCode;
    private final String applicationTitle;
    private final boolean feeRequired;
    private final String result;
    private final UUID applicationListId;
    private final LocalDate date;
    private final UUID listId;
}
