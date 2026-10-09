package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

public record ApplicationCodeIngressRecord(
        Long id,
        String code,
        String title,
        String wording,
        String legislation,
        YesOrNo feeDue,
        YesOrNo requiresRespondent,
        LocalDate startDate,
        LocalDate endDate,
        YesOrNo bulkRespondentAllowed,
        Long version,
        String feeReference) {

    static ApplicationCodeIngressRecord fromEntity(ApplicationCode applicationCode) {
        return new ApplicationCodeIngressRecord(
                applicationCode.getId(),
                applicationCode.getCode(),
                applicationCode.getTitle(),
                applicationCode.getWording(),
                applicationCode.getLegislation(),
                applicationCode.getFeeDue(),
                applicationCode.getRequiresRespondent(),
                applicationCode.getStartDate(),
                applicationCode.getEndDate(),
                applicationCode.getBulkRespondentAllowed(),
                applicationCode.getVersion(),
                applicationCode.getFeeReference());
    }

    public ApplicationCodeIngressRecord withId(Long resolvedId) {
        return new ApplicationCodeIngressRecord(
                resolvedId,
                code,
                title,
                wording,
                legislation,
                feeDue,
                requiresRespondent,
                startDate,
                endDate,
                bulkRespondentAllowed,
                version,
                feeReference);
    }
}
