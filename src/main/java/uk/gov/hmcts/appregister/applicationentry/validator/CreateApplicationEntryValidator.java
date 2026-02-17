package uk.gov.hmcts.appregister.applicationentry.validator;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.validator.regex.DisallowedCharactersRegexValidator;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Respondent;

/**
 * Validates the dto for an application entry create.
 */
@Component
@Slf4j
public class CreateApplicationEntryValidator
        extends AbstractApplicationEntryValidator<
                PayloadForCreate<EntryCreateDto>, CreateApplicationEntryValidationSuccess> {

    public CreateApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            FeeRepository feeRepository,
            Clock clock,
            StandardApplicantRepository standardApplicantRepository,
            DisallowedCharactersRegexValidator disallowedCharactersRegexValidator) {
        super(
                applicationListRepository,
                applicationCodeRepository,
                feeRepository,
                clock,
                standardApplicantRepository,
                disallowedCharactersRegexValidator);
    }

    @Override
    protected CreateApplicationEntryValidationSuccess getResult(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            Fee fee,
            StandardApplicant saCode,
            ApplicationList applicationList,
            PayloadForCreate<EntryCreateDto> dto) {
        return CreateApplicationEntryValidationSuccess.builder()
                .applicationCode(code)
                .wordingSentence(wordingTemplateCollection)
                .fee(fee)
                .sa(saCode)
                .applicationList(applicationList)
                .build();
    }

    @Override
    protected Respondent getRespondent(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getRespondent();
    }

    @Override
    protected Applicant getApplicant(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getApplicant();
    }

    @Override
    protected String getApplicationCode(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getApplicationCode();
    }

    @Override
    protected List<FeeStatus> getFeeStatuses(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getFeeStatuses();
    }

    @Override
    protected Boolean getHasOffsiteFee(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getHasOffsiteFee();
    }

    @Override
    protected UUID getApplicationListUuid(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getId();
    }

    @Override
    protected String getStandardApplicantCode(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getStandardApplicantCode();
    }

    @Override
    protected Integer getNumberOfRespondents(PayloadForCreate<EntryCreateDto> validatable) {
        return validatable.getData().getNumberOfRespondents();
    }
}
