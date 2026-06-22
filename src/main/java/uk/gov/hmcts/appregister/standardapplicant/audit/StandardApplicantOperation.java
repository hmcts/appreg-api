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
    CREATE_STANDARD_APPLICANT("Create Standard Applicant", CrudEnum.CREATE),
    UPDATE_STANDARD_APPLICANT("Update Standard Applicant", CrudEnum.UPDATE),
    DELETE_STANDARD_APPLICANT("Delete Standard Applicant", CrudEnum.DELETE);

    private final String eventName;

    private final CrudEnum type;
}
