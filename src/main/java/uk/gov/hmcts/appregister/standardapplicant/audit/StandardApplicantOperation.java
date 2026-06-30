package uk.gov.hmcts.appregister.standardapplicant.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum StandardApplicantOperation implements AuditOperation {
    GET_STANDARD_APPLICANTS("Get Standard Applicants", CrudEnum.READ),
    GET_STANDARD_APPLICANT_BY_CODE("Get Standard Applicant by code", CrudEnum.READ),
    PRINT_STANDARD_APPLICANTS("Print Standard Applicants", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
