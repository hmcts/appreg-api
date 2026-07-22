package uk.gov.hmcts.appregister.applicationentry.validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.Respondent;

/**
 * Validates the dto for an application entry update.
 */
@Component
@Slf4j
public class UpdateApplicationEntryValidator
        extends AbstractApplicationEntryValidator<
                PayloadForUpdateEntry, UpdateApplicationEntryValidationSuccess> {
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    public UpdateApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            ApplicationFeeService feeService,
            BusinessDateProvider businessDateProvider,
            StandardApplicantRepository standardApplicantRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            AppListEntryFeeStatusRepository appListEntryFeeStatusRepository) {
        super(
                applicationListRepository,
                applicationCodeRepository,
                feeService,
                businessDateProvider,
                standardApplicantRepository);
        this.applicationListEntryRepository = applicationListEntryRepository;
        this.appListEntryFeeStatusRepository = appListEntryFeeStatusRepository;
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

        return super.validate(
                validatable,
                (payload, success) -> {
                    validateFeeStatusTransition(success.getApplicationCode(), payload);
                    return validateSuccess == null ? null : validateSuccess.apply(payload, success);
                });
    }

    private void validateFeeStatusTransition(
            ApplicationCode applicationCode, PayloadForUpdateEntry validatable) {
        if (applicationCode.getFeeDue() == YesOrNo.YES) {
            return;
        }

        List<FeeStatus> requestedFeeStatuses = validatable.getData().getFeeStatuses();
        if (requestedFeeStatuses == null || !requestedFeeStatuses.isEmpty()) {
            return;
        }

        boolean hasPersistedFeeStatuses =
                !appListEntryFeeStatusRepository
                        .getFeeStatusByEntryUuid(validatable.getEntryId())
                        .isEmpty();

        if (hasPersistedFeeStatuses) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_NOT_REQUIRED,
                    "Fee not required for code %s".formatted(getApplicationCode(validatable)));
        }
    }

    @Override
    protected boolean isRetainedFeeStatusAllowed(
            ApplicationCode applicationCode,
            PayloadForUpdateEntry validatable,
            List<FeeStatus> requestedFeeStatuses) {
        if (applicationCode.getFeeDue() == YesOrNo.YES
                || requestedFeeStatuses.stream().anyMatch(Objects::isNull)) {
            return false;
        }

        List<AppListEntryFeeStatus> persistedFeeStatuses =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(validatable.getEntryId());
        if (persistedFeeStatuses.isEmpty()) {
            return false;
        }

        return toRequestedFeeStatusCounts(requestedFeeStatuses)
                .equals(toPersistedFeeStatusCounts(persistedFeeStatuses));
    }

    private Map<FeeStatusSnapshot, Long> toRequestedFeeStatusCounts(List<FeeStatus> feeStatuses) {
        return feeStatuses.stream()
                .map(
                        feeStatus ->
                                new FeeStatusSnapshot(
                                        feeStatus.getPaymentStatus(),
                                        feeStatus.getStatusDate(),
                                        normalisePaymentReference(feeStatus.getPaymentReference())))
                .collect(Collectors.groupingBy(snapshot -> snapshot, Collectors.counting()));
    }

    private Map<FeeStatusSnapshot, Long> toPersistedFeeStatusCounts(
            List<AppListEntryFeeStatus> feeStatuses) {
        return feeStatuses.stream()
                .map(
                        feeStatus ->
                                new FeeStatusSnapshot(
                                        toPaymentStatus(feeStatus.getAlefsFeeStatus()),
                                        feeStatus.getAlefsFeeStatusDate(),
                                        normalisePaymentReference(
                                                feeStatus.getAlefsPaymentReference())))
                .collect(Collectors.groupingBy(snapshot -> snapshot, Collectors.counting()));
    }

    private String normalisePaymentReference(String paymentReference) {
        if (paymentReference == null || paymentReference.isBlank()) {
            return null;
        }
        return paymentReference;
    }

    private PaymentStatus toPaymentStatus(FeeStatusType feeStatus) {
        if (feeStatus == null) {
            return null;
        }

        return switch (feeStatus) {
            case DUE -> PaymentStatus.DUE;
            case PAID -> PaymentStatus.PAID;
            case REMITTED -> PaymentStatus.REMITTED;
            case UNDERTAKING -> PaymentStatus.UNDERTAKEN;
        };
    }

    private record FeeStatusSnapshot(
            PaymentStatus paymentStatus, LocalDate statusDate, String paymentReference) {}

    @Override
    protected UpdateApplicationEntryValidationSuccess getResult(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            FeePair fee,
            StandardApplicant saCode,
            ApplicationList applicationList,
            PayloadForUpdateEntry payload) {
        return new UpdateApplicationEntryValidationSuccess(
                wordingTemplateCollection,
                code,
                fee,
                saCode,
                applicationList,
                applicationListEntryRepository.findByUuid(payload.getEntryId()).orElse(null));
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
    protected List<Official> getOfficials(PayloadForUpdateEntry validatable) {
        return validatable.getData().getOfficials();
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

    @Override
    protected String getAccountNumber(PayloadForUpdateEntry validatable) {
        return validatable.getData().getAccountNumber();
    }

    @Override
    protected LocalDate getLodgementDate(PayloadForUpdateEntry validatable) {
        Optional<ApplicationListEntry> ale =
                applicationListEntryRepository.findByUuid(validatable.getEntryId());
        return ale.map(ApplicationListEntry::getLodgementDate).orElse(null);
    }
}
