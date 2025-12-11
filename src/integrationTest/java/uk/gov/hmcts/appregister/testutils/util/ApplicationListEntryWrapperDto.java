package uk.gov.hmcts.appregister.testutils.util;

import java.time.LocalDate;
import java.util.List;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.Respondent;

/**
 * Defines a common class that wraps a EntryCreateDto or a EntryUpdateDto to provide a unified
 * interface for accessing their properties. Can be used with {@link ApplicationListEntryAssertion}.
 */
public class ApplicationListEntryWrapperDto {
    private EntryCreateDto entryCreateDto;
    private EntryUpdateDto entryUpdateDto;

    public ApplicationListEntryWrapperDto(EntryCreateDto entryCreateDto) {
        this.entryCreateDto = entryCreateDto;
    }

    public ApplicationListEntryWrapperDto(EntryUpdateDto entryUpdateDto) {
        this.entryUpdateDto = entryUpdateDto;
    }

    public String getStandardApplicantCode() {
        if (entryCreateDto != null) {
            return entryCreateDto.getStandardApplicantCode();
        } else {
            return entryUpdateDto.getStandardApplicantCode();
        }
    }

    public Applicant getApplicant() {
        if (entryCreateDto != null) {
            return entryCreateDto.getApplicant();
        } else {
            return entryUpdateDto.getApplicant();
        }
    }

    public Respondent getRespondent() {
        if (entryCreateDto != null) {
            return entryCreateDto.getRespondent();
        } else {
            return entryUpdateDto.getRespondent();
        }
    }

    public Integer getNumberOfRespondents() {
        if (entryCreateDto != null) {
            return entryCreateDto.getNumberOfRespondents();
        } else {
            return entryUpdateDto.getNumberOfRespondents();
        }
    }

    public List<FeeStatus> getFeeStatuses() {
        if (entryCreateDto != null) {
            return entryCreateDto.getFeeStatuses();
        } else {
            return entryUpdateDto.getFeeStatuses();
        }
    }

    public Boolean getHasOffsiteFee() {
        if (entryCreateDto != null) {
            return entryCreateDto.getHasOffsiteFee();
        } else {
            return entryUpdateDto.getHasOffsiteFee();
        }
    }

    public String getCaseReference() {
        if (entryCreateDto != null) {
            return entryCreateDto.getCaseReference();
        } else {
            return entryUpdateDto.getCaseReference();
        }
    }

    public String getNotes() {
        if (entryCreateDto != null) {
            return entryCreateDto.getNotes();
        } else {
            return entryUpdateDto.getNotes();
        }
    }

    public String getAccountNumber() {
        if (entryCreateDto != null) {
            return entryCreateDto.getAccountNumber();
        } else {
            return entryUpdateDto.getAccountNumber();
        }
    }

    public LocalDate getLodgementDate() {
        if (entryCreateDto != null) {
            return entryCreateDto.getLodgementDate();
        } else {
            return entryUpdateDto.getLodgementDate();
        }
    }

    public List<Official> getOfficials() {
        if (entryCreateDto != null) {
            return entryCreateDto.getOfficials();
        } else {
            return entryUpdateDto.getOfficials();
        }
    }
}
