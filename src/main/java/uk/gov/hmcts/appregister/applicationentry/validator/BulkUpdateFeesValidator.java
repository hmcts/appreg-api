package uk.gov.hmcts.appregister.applicationentry.validator;

import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUpdateFeesPayload;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;

@Component
public class BulkUpdateFeesValidator
        implements Validator<BulkUpdateFeesPayload, BulkUpdateFeesValidationSuccess> {

    private static final int PAYMENT_REFERENCE_MAX_LENGTH = 100;

    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final BusinessDateProvider businessDateProvider;
    private final jakarta.validation.Validator beanValidator;

    public BulkUpdateFeesValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            BusinessDateProvider businessDateProvider,
            jakarta.validation.Validator beanValidator) {
        this.applicationListRepository = applicationListRepository;
        this.applicationListEntryRepository = applicationListEntryRepository;
        this.businessDateProvider = businessDateProvider;
        this.beanValidator = beanValidator;
    }

    @Override
    public void validate(BulkUpdateFeesPayload payload) {
        validate(payload, (req, success) -> null);
    }

    @Override
    public <R> R validate(
            BulkUpdateFeesPayload payload,
            BiFunction<BulkUpdateFeesPayload, BulkUpdateFeesValidationSuccess, R> validateSuccess) {
        validateApplicationList(payload.listId());
        Set<UUID> requestedIds = validateEntryIds(payload);

        if (payload.data().getFeeDetails().isPresent() && !payload.data().getFeeDetails().get().isEmpty()) {
            validateFeeDetails(payload);
        }

        List<ApplicationListEntry> entries =
                applicationListEntryRepository.findByUuidsInSourceList(
                        payload.listId(), requestedIds);
        validateAllEntriesBelongToList(payload.listId(), requestedIds, entries);

        var success = new BulkUpdateFeesValidationSuccess();
        success.setEntries(entries);

        return validateSuccess.apply(payload, success);
    }

    private void validateApplicationList(UUID listId) {
        ApplicationList applicationList =
                applicationListRepository
                        .findByUuidIncludingDelete(listId)
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                                                "The application list does not exist %s"
                                                        .formatted(listId)));

        if (applicationList.getStatus() != Status.OPEN || applicationList.isDeleted()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list id %s is not in the correct state or the application list is deleted %s"
                            .formatted(listId, applicationList.getStatus()));
        }
    }

    private Set<UUID> validateEntryIds(BulkUpdateFeesPayload payload) {
        if (payload.data() == null || isNullOrEmpty(payload.data().getEntryIds())) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided");
        }

        return new HashSet<>(payload.data().getEntryIds());
    }

    private void validateFeeDetails(BulkUpdateFeesPayload payload) {
        if (payload.data() == null || isNullOrEmpty(payload.data().getFeeDetails().get())) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_DETAILS_NOT_PROVIDED, "No fee details provided");
        }

        for (BulkFeeDetailsDto feeDetails : payload.data().getFeeDetails().get()) {
            validateFeeDetail(feeDetails);
        }
    }

    private void validateFeeDetail(BulkFeeDetailsDto feeDetails) {
        if (feeDetails == null) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_DETAILS_NOT_PROVIDED, "Fee detail must be provided");
        }

        validateRequiredFeeDetailFields(feeDetails);

        if (feeDetails.getPaymentStatus() == DUE
                && StringUtils.isNotBlank(feeDetails.getPaymentReference())) {
            throw new AppRegistryException(
                    AppListEntryError.PAYMENT_REFERENCE_NOT_ALLOWED_WHEN_PAYMENT_DUE,
                    "Payment reference must not be provided when fee status is DUE");
        }

        if (feeDetails.getStatusDate().isAfter(businessDateProvider.currentUkDate())) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_STATUS_DATE_CANNOT_BE_IN_FUTURE,
                    "statusDate cannot be in the future");
        }

        if (feeDetails.getPaymentReference() != null
                && feeDetails.getPaymentReference().length() > PAYMENT_REFERENCE_MAX_LENGTH) {
            throw new AppRegistryException(
                    AppListEntryError.PAYMENT_REFERENCE_TOO_LONG,
                    "paymentReference must not be longer than %s characters"
                            .formatted(PAYMENT_REFERENCE_MAX_LENGTH));
        }
    }

    private void validateRequiredFeeDetailFields(BulkFeeDetailsDto feeDetails) {
        Set<String> missingFields =
                beanValidator.validate(feeDetails).stream()
                        .filter(violation -> violation.getInvalidValue() == null)
                        .map(ConstraintViolation::getPropertyPath)
                        .map(Object::toString)
                        .collect(Collectors.toSet());

        if (missingFields.contains("paymentStatus")) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_PAYMENT_STATUS_REQUIRED,
                    "paymentStatus must be provided");
        }

        if (missingFields.contains("statusDate")) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_STATUS_DATE_REQUIRED, "statusDate must be provided");
        }
    }

    private void validateAllEntriesBelongToList(
            UUID listId, Set<UUID> requestedIds, List<ApplicationListEntry> entries) {
        Set<UUID> existingIds =
                entries.stream().map(ApplicationListEntry::getUuid).collect(Collectors.toSet());

        if (existingIds.size() == requestedIds.size()) {
            return;
        }

        List<EntryToList> entryToLists =
                applicationListEntryRepository.findApplicationListForAllEntries(
                        new ArrayList<>(requestedIds));

        Set<UUID> knownIds =
                entryToLists.stream().map(EntryToList::entryId).collect(Collectors.toSet());
        Set<UUID> wrongListIds =
                entryToLists.stream()
                        .filter(entryToList -> !listId.equals(entryToList.listId()))
                        .map(EntryToList::entryId)
                        .collect(Collectors.toSet());

        if (!wrongListIds.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_NOT_ACCESSIBLE_FOR_LIST,
                    "One or more entries do not belong to the application list",
                    Map.of("invalid_entry_ids", wrongListIds.toString()));
        }

        Set<UUID> missingIds = new HashSet<>(requestedIds);
        missingIds.removeAll(knownIds);

        throw new AppRegistryException(
                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                "One or more entries were not found in the source list",
                Map.of("invalid_entry_ids", missingIds.toString()));
    }

    private boolean isNullOrEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
