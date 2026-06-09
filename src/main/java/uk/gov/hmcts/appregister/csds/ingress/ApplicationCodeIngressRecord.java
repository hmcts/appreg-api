package uk.gov.hmcts.appregister.csds.ingress;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

record ApplicationCodeIngressRecord(
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

    String toCsvRow() {
        return String.join(
                        ",",
                        ApplicationCodeDiffReportingService.csvValue(id),
                        ApplicationCodeDiffReportingService.csvValue(code),
                        ApplicationCodeDiffReportingService.csvValue(title),
                        ApplicationCodeDiffReportingService.csvValue(wording),
                        ApplicationCodeDiffReportingService.csvValue(legislation),
                        ApplicationCodeDiffReportingService.csvValue(feeDue),
                        ApplicationCodeDiffReportingService.csvValue(requiresRespondent),
                        ApplicationCodeDiffReportingService.csvValue(startDate),
                        ApplicationCodeDiffReportingService.csvValue(endDate),
                        ApplicationCodeDiffReportingService.csvValue(bulkRespondentAllowed),
                        ApplicationCodeDiffReportingService.csvValue(version),
                        ApplicationCodeDiffReportingService.csvValue(feeReference))
                + "\n";
    }

    String toCsvRow(Long referenceCount) {
        return String.join(
                        ",",
                        ApplicationCodeDiffReportingService.csvValue(id),
                        ApplicationCodeDiffReportingService.csvValue(code),
                        ApplicationCodeDiffReportingService.csvValue(title),
                        ApplicationCodeDiffReportingService.csvValue(wording),
                        ApplicationCodeDiffReportingService.csvValue(legislation),
                        ApplicationCodeDiffReportingService.csvValue(feeDue),
                        ApplicationCodeDiffReportingService.csvValue(requiresRespondent),
                        ApplicationCodeDiffReportingService.csvValue(startDate),
                        ApplicationCodeDiffReportingService.csvValue(endDate),
                        ApplicationCodeDiffReportingService.csvValue(bulkRespondentAllowed),
                        ApplicationCodeDiffReportingService.csvValue(version),
                        ApplicationCodeDiffReportingService.csvValue(feeReference),
                        ApplicationCodeDiffReportingService.csvValue(referenceCount))
                + "\n";
    }
}
