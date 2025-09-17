package uk.gov.hmcts.appregister.applicationentry.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationListEntryDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;
import uk.gov.hmcts.appregister.applicationentry.dto.IdentityDetailsWriteDto;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapper;

@RequiredArgsConstructor
@Component
public class ApplicationListEntryMapper {

    private final StandardApplicantMapper standardApplicantMapper;
    private final ApplicationCodeMapper applicationCodeMapper;
    private final NameAddressMapper identityDetailsMapper;
    public static final String TRUE_VALUE = "1";

    public ApplicationListEntryDto toReadDto(
            ApplicationListEntry entity, uk.gov.hmcts.appregister.common.entity.FeePair fees) {

        AppListEntryFeeStatus mainFeeRecord =
                entity.getEntryFeeStatuses().stream().findFirst().orElse(null);

        return new ApplicationListEntryDto(
                entity.getId(),
                standardApplicantMapper.toReadDto(entity.getStandardApplicant()),
                applicationCodeMapper.toReadDto(entity.getApplicationCode(), fees),
                mainFeeRecord != null
                        ? FeeStatusType.fromDisplayName(mainFeeRecord.getAlefsFeeStatus())
                        : null,
                mainFeeRecord != null ? mainFeeRecord.getAlefsPaymentReference() : null,
                entity.getStandardApplicant() != null
                        ? identityDetailsMapper.toReadDto(entity.getAnamedaddress())
                        : null,
                entity.getStandardApplicant() != null
                        ? identityDetailsMapper.toReadDto(entity.getRnameaddress())
                        : null,
                entity.getNumberOfBulkRespondents(),
                entity.getApplicationListEntryWording(),
                entity.getCaseReference(),
                entity.getAccountNumber(),
                entity.getEntryRescheduled(),
                entity.getNotes(),
                entity.getBulkUpload(),
                // TODO: Fix this when results are implemented
                0L,
                entity.getChangedBy(),
                entity.getChangedDate(),
                entity.getVersion());
    }

    public ApplicationListEntry createFromWriteDto(
            ApplicationWriteDto dto,
            StandardApplicant applicant,
            String wording,
            ApplicationCode code) {
        return ApplicationListEntry.builder()
                .standardApplicant(applicant)
                .applicationCode(code)
                .rnameaddress(
                        dto.respondent() != null
                                ? identityDetailsMapper.createFromWriteDto(dto.respondent())
                                : null)
                .anamedaddress(
                        dto.respondent() != null
                                ? identityDetailsMapper.createFromWriteDto(dto.applicant())
                                : null)
                .numberOfBulkRespondents(dto.numberOfBulkRespondents())
                .applicationListEntryWording(wording)
                .caseReference(dto.caseReference())
                .accountNumber(dto.accountNumber())
                .entryRescheduled(dto.applicationRescheduled())
                .notes(dto.notes())
                .bulkUpload(dto.bulkUpload())
                .build();
    }

    public void updateFromWriteDto(
            ApplicationWriteDto dto,
            ApplicationListEntry entity,
            StandardApplicant applicant,
            String wording,
            ApplicationCode code) {
        entity.setStandardApplicant(applicant);
        entity.setApplicationCode(code);

        entity.setAnamedaddress(handleIdentityUpdate(dto.applicant(), entity.getAnamedaddress()));
        entity.setRnameaddress(handleIdentityUpdate(dto.respondent(), entity.getRnameaddress()));

        entity.setNumberOfBulkRespondents(dto.numberOfBulkRespondents());
        entity.setApplicationListEntryWording(wording);
        entity.setCaseReference(dto.caseReference());
        entity.setAccountNumber(dto.accountNumber());
        entity.setEntryRescheduled("CANT FIND RESULT");
        entity.setNotes(dto.notes());
        entity.setBulkUpload(dto.bulkUpload());
    }

    private NameAddress handleIdentityUpdate(IdentityDetailsWriteDto dto, NameAddress current) {
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
