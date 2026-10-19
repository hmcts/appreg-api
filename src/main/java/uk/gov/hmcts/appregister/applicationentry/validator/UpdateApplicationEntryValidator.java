package uk.gov.hmcts.appregister.applicationentry.validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
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
    private final AppListEntryFeeRepository appListEntryFeeRepository;

    public UpdateApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            ApplicationFeeService feeService,
            BusinessDateProvider businessDateProvider,
            StandardApplicantRepository standardApplicantRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            AppListEntryFeeStatusRepository appListEntryFeeStatusRepository,
            AppListEntryFeeRepository appListEntryFeeRepository) {
        super(
                applicationListRepository,
                applicationCodeRepository,
                feeService,
                businessDateProvider,
                standardApplicantRepository);
        this.applicationListEntryRepository = applicationListEntryRepository;
        this.appListEntryFeeStatusRepository = appListEntryFeeStatusRepository;
        this.appListEntryFeeRepository = appListEntryFeeRepository;
    }

    @Override
    protected FeePair resolveFee(ApplicationCode applicationCode, PayloadForUpdateEntry payload) {
        var entry = payload.getApplicationListEntry();
        if (applicationCode.getFeeDue() != YesOrNo.YES
                || entry.getApplicationCode() == null
                || !entry.getApplicationCode()
                        .getCode()
                        .equalsIgnoreCase(getApplicationCode(payload))) {
            return super.resolveFee(applicationCode, payload);
        }
        // Existing fee mappings are historical references, not candidates for date-based
        // reselection.
        var stored = appListEntryFeeRepository.getFeeForEntryId(entry.getId());
        if (stored.isEmpty()) {
            return super.resolveFee(applicationCode, payload);
        }
        var main = stored.stream().filter(fee -> !fee.isOffsite()).findFirst();
        var offsite = stored.stream().filter(Fee::isOffsite).findFirst();
        if (main.isPresent()
                && (offsite.isPresent() || !Boolean.TRUE.equals(getHasOffsiteFee(payload)))) {
            return new FeePair(main.get(), offsite.orElse(null));
        }
        var selected = super.resolveFee(applicationCode, payload);
        return new FeePair(main.orElse(selected.mainFee()), offsite.orElse(selected.offsiteFee()));
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
        final ApplicationList applicationList = validateParentApplicationList(validatable.getId());

        var entry =
                applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        validatable.getId(), validatable.getEntryId());
        if (entry.isEmpty()
                && applicationListEntryRepository.findByUuid(validatable.getEntryId()).isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_DOES_NOT_EXIST,
                    "The application entry %s does not exist in application list %s"
                            .formatted(
                                    validatable.getEntryId(), getApplicationListUuid(validatable)));
        }

        log.debug(" application list entry is found {}", validatable.getEntryId());

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

        var resolvedPayload = validatable.withApplicationListEntry(entry.orElseThrow());
        return validateUsingApplicationList(
                resolvedPayload,
                (payload, success) -> {
                    validateFeeStatusTransition(success.getApplicationCode(), payload);
                    return validateSuccess == null ? null : validateSuccess.apply(payload, success);
                },
                applicationList);
    }

    @Override
    protected StandardApplicant validateStandardApplicantCode(PayloadForUpdateEntry validatable) {
        var applicant = validatable.getApplicationListEntry().getStandardApplicant();
        if (applicant != null
                && applicant
                        .getApplicantCode()
                        .equalsIgnoreCase(getStandardApplicantCode(validatable))) {
            // Preserve the stored version when only other entry details are edited.
            return applicant;
        }
        return super.validateStandardApplicantCode(validatable);
    }

    @Override
    protected ApplicationCode validateApplicationCode(PayloadForUpdateEntry validatable) {
        var existingCode = validatable.getApplicationListEntry().getApplicationCode();
        // Retain the associated configuration, even after expiry; replacements must be active.
        if (existingCode != null
                && existingCode.getCode().equalsIgnoreCase(getApplicationCode(validatable))) {
            return existingCode;
        }
        return super.validateApplicationCode(validatable);
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
                payload.getApplicationListEntry());
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
        return validatable.getApplicationListEntry().getLodgementDate();
    }
}
