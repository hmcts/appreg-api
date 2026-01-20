package uk.gov.hmcts.appregister.applicationentry.validator;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Respondent;

/**
 * Validates the dto for an application entry update.
 */
@Component
@Slf4j
public class UpdateApplicationEntryValidator
        extends AbstractApplicatonEntryValidator<
                PayloadForUpdateEntry, UpdateApplicationEntryValidationSuccess> {
    private final ApplicationListEntryRepository applicationListEntryRepository;

    public UpdateApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            FeeRepository feeRepository,
            Clock clock,
            StandardApplicantRepository standardApplicantRepository,
            ApplicationListEntryRepository applicationListEntryRepository) {
        super(
                applicationListRepository,
                applicationCodeRepository,
                feeRepository,
                clock,
                standardApplicantRepository);
        this.applicationListEntryRepository = applicationListEntryRepository;
    }

    @Override
    public void validate(PayloadForUpdateEntry validatable) {
        validate(validatable, null);
    }

    @Override
    public <R> R validate(
            PayloadForUpdateEntry validatable,
            BiFunction<PayloadForUpdateEntry, UpdateApplicationEntryValidationSuccess, R>
                    validateSuccess) {
        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findByUuid(validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_DOES_NOT_EXIST,
                    "The application entry %s does not exist in application list %s"
                            .formatted(
                                    validatable.getEntryId(), getApplicationListUuid(validatable)));
        }

        log.debug(" application list entry is found {}", validatable.getEntryId());

        entry =
                applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        validatable.getId(), validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST,
                    "The application list entry does not exist %s"
                            .formatted(getApplicationListEntryUuid(validatable)));
        }

        log.debug(
                " application list entry {} is found and is within list {}",
                validatable.getEntryId(),
                validatable.getId());

        return super.validate(validatable, validateSuccess);
    }

    @Override
    protected UpdateApplicationEntryValidationSuccess getResult(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            Fee fee,
            StandardApplicant saCode,
            ApplicationList applicationList,
            PayloadForUpdateEntry payload) {
        return new UpdateApplicationEntryValidationSuccess(
                wordingTemplateCollection,
                code,
                fee,
                saCode,
                applicationList,
                applicationListEntryRepository.findByUuid(payload.getEntryId()).get());
    }

    @Override
    protected Respondent getRespondent(PayloadForUpdateEntry validatable) {
        return validatable.getData().getRespondent();
    }

    @Override
    protected Applicant getApplicant(PayloadForUpdateEntry validatable) {
        return validatable.getData().getApplicant();
    }

    @Override
    protected String getApplicationCode(PayloadForUpdateEntry validatable) {
        return validatable.getData().getApplicationCode();
    }

    @Override
    protected List<FeeStatus> getFeeStatuses(PayloadForUpdateEntry validatable) {
        return validatable.getData().getFeeStatuses();
    }

    @Override
    protected Boolean getHasOffsiteFee(PayloadForUpdateEntry validatable) {
        return validatable.getData().getHasOffsiteFee();
    }

    @Override
    protected UUID getApplicationListUuid(PayloadForUpdateEntry validatable) {
        return validatable.getId();
    }

    protected UUID getApplicationListEntryUuid(PayloadForUpdateEntry validatable) {
        return validatable.getEntryId();
    }

    @Override
    protected String getStandardApplicantCode(PayloadForUpdateEntry validatable) {
        return validatable.getData().getStandardApplicantCode();
    }

    @Override
    protected Integer getNumberOfRespondents(PayloadForUpdateEntry validatable) {
        return validatable.getData().getNumberOfRespondents();
    }
}
