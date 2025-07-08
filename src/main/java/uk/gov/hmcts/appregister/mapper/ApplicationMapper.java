package uk.gov.hmcts.appregister.mapper;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.internal.FeePair;
import uk.gov.hmcts.appregister.dto.read.ApplicationDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationWriteDto;
import uk.gov.hmcts.appregister.dto.write.IdentityDetailsWriteDto;
import uk.gov.hmcts.appregister.model.Application;
import uk.gov.hmcts.appregister.model.ApplicationCode;
import uk.gov.hmcts.appregister.model.ApplicationFeeRecord;
import uk.gov.hmcts.appregister.model.IdentityDetails;
import uk.gov.hmcts.appregister.model.StandardApplicant;
import uk.gov.hmcts.appregister.service.VersionManager;

@RequiredArgsConstructor
@Component
public class ApplicationMapper {

    private final StandardApplicantMapper standardApplicantMapper;
    private final ApplicationCodeMapper applicationCodeMapper;
    private final IdentityDetailsMapper identityDetailsMapper;
    private final VersionManager versionManager;

    public ApplicationDto toReadDto(Application entity, FeePair fees) {

        ApplicationFeeRecord mainFeeRecord =
                entity.getFeeRecords().stream()
                        .filter(
                                r ->
                                        r.getApplicationFee() != null
                                                && !r.getApplicationFee().isOffset())
                        .findFirst()
                        .orElse(null);

        return new ApplicationDto(
                entity.getId(),
                standardApplicantMapper.toReadDto(entity.getStandardApplicant()),
                applicationCodeMapper.toReadDto(entity.getApplicationCode(), fees),
                mainFeeRecord != null ? mainFeeRecord.getFeeStatus() : null,
                mainFeeRecord != null ? mainFeeRecord.getPaymentReference() : null,
                entity.getApplicant() != null
                        ? identityDetailsMapper.toReadDto(entity.getApplicant())
                        : null,
                entity.getRespondent() != null
                        ? identityDetailsMapper.toReadDto(entity.getRespondent())
                        : null,
                entity.getNumberOfBulkRespondents(),
                entity.getApplicationWording(),
                entity.getCaseReference(),
                entity.getAccountNumber(),
                entity.getApplicationRescheduled(),
                entity.getNotes(),
                entity.getBulkUpload(),
                entity.getResult() != null ? entity.getResult().getId() : null,
                entity.getChangedBy(),
                entity.getChangedDate(),
                entity.getVersion());
    }

    public Application createFromWriteDto(
            ApplicationWriteDto dto,
            StandardApplicant applicant,
            String wording,
            ApplicationCode code,
            String userId,
            LocalDate changedDate) {
        return Application.builder()
                .standardApplicant(applicant)
                .applicationCode(code)
                .applicant(
                        dto.applicant() != null
                                ? identityDetailsMapper.createFromWriteDto(dto.applicant())
                                : null)
                .respondent(
                        dto.respondent() != null
                                ? identityDetailsMapper.createFromWriteDto(dto.respondent())
                                : null)
                .numberOfBulkRespondents(dto.numberOfBulkRespondents())
                .applicationWording(wording)
                .caseReference(dto.caseReference())
                .accountNumber(dto.accountNumber())
                .applicationRescheduled(dto.applicationRescheduled())
                .notes(dto.notes())
                .bulkUpload(dto.bulkUpload())
                .changedBy(userId)
                .changedDate(changedDate)
                .version(versionManager.increment(0))
                .build();
    }

    public void updateFromWriteDto(
            ApplicationWriteDto dto,
            Application entity,
            StandardApplicant applicant,
            String wording,
            ApplicationCode code,
            String userId,
            LocalDate changedDate) {
        entity.setStandardApplicant(applicant);
        entity.setApplicationCode(code);

        entity.setApplicant(handleIdentityUpdate(dto.applicant(), entity.getApplicant()));
        entity.setRespondent(handleIdentityUpdate(dto.respondent(), entity.getRespondent()));

        entity.setNumberOfBulkRespondents(dto.numberOfBulkRespondents());
        entity.setApplicationWording(wording);
        entity.setCaseReference(dto.caseReference());
        entity.setAccountNumber(dto.accountNumber());
        entity.setApplicationRescheduled(dto.applicationRescheduled());
        entity.setNotes(dto.notes());
        entity.setVersion(versionManager.increment(entity.getVersion()));
        entity.setBulkUpload(dto.bulkUpload());
        entity.setChangedBy(userId);
        entity.setChangedDate(changedDate);
    }

    private IdentityDetails handleIdentityUpdate(
            IdentityDetailsWriteDto dto, IdentityDetails current) {
        if (dto == null) {
            return null;
        }

        if (current == null) {
            return identityDetailsMapper.createFromWriteDto(dto);
        }

        identityDetailsMapper.updateFromWriteDto(dto, current);
        return current;
    }
}
