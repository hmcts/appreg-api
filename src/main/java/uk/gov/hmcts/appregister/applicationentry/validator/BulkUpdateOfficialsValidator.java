package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUpdateOfficialsPayload;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

@Component
@RequiredArgsConstructor
public class BulkUpdateOfficialsValidator
        implements Validator<BulkUpdateOfficialsPayload, BulkUpdateOfficialsValidationSuccess> {

    private static final int MAX_MAGISTRATES = 3;
    private static final int MAX_COURT_OFFICIALS = 1;

    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;

    @Override
    public void validate(BulkUpdateOfficialsPayload payload) {
        validate(payload, (req, success) -> null);
    }

    @Override
    public <R> R validate(
            BulkUpdateOfficialsPayload payload,
            BiFunction<BulkUpdateOfficialsPayload, BulkUpdateOfficialsValidationSuccess, R>
                    validateSuccess) {
        validateApplicationList(payload.listId());
        Set<UUID> requestedIds = validateEntryIds(payload);
        validateOfficials(payload);

        List<ApplicationListEntry> entries =
                applicationListEntryRepository.findByUuidsInSourceList(
                        payload.listId(), requestedIds);
        validateAllEntriesFound(requestedIds, entries);

        var success = new BulkUpdateOfficialsValidationSuccess();
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

    private Set<UUID> validateEntryIds(BulkUpdateOfficialsPayload payload) {
        if (payload.data() == null || isNullOrEmpty(payload.data().getEntryIds())) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided");
        }

        List<UUID> entryIds = payload.data().getEntryIds().stream().toList();
        Set<UUID> uniqueEntryIds = new HashSet<>(entryIds);

        if (uniqueEntryIds.size() != entryIds.size()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE,
                    "Duplicate entry IDs are not allowed");
        }

        return uniqueEntryIds;
    }

    private void validateOfficials(BulkUpdateOfficialsPayload payload) {
        if (payload.data() == null || isNull(payload.data().getOfficials())) {
            throw new AppRegistryException(
                    AppListEntryError.OFFICIALS_NOT_PROVIDED, "No officials provided");
        }

        List<Official> officials = payload.data().getOfficials();
        List<Integer> invalidOfficialIndexes = new ArrayList<>();
        for (int i = 0; i < officials.size(); i++) {
            if (officials.get(i) == null || officials.get(i).getType() == null) {
                invalidOfficialIndexes.add(i);
            }
        }

        if (!invalidOfficialIndexes.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.OFFICIAL_TYPE_REQUIRED,
                    "Officials must include a type at indexes %s"
                            .formatted(invalidOfficialIndexes));
        }

        long magistrateCount =
                officials.stream()
                        .filter(official -> official.getType() == OfficialType.MAGISTRATE)
                        .count();
        if (magistrateCount > MAX_MAGISTRATES) {
            throw new AppRegistryException(
                    AppListEntryError.TOO_MANY_MAGISTRATES,
                    "An application entry can include no more than %s Magistrates"
                            .formatted(MAX_MAGISTRATES));
        }

        long courtOfficialCount =
                officials.stream()
                        .filter(official -> official.getType() == OfficialType.CLERK)
                        .count();
        if (courtOfficialCount > MAX_COURT_OFFICIALS) {
            throw new AppRegistryException(
                    AppListEntryError.TOO_MANY_COURT_OFFICIALS,
                    "An application entry can include no more than %s Court Official"
                            .formatted(MAX_COURT_OFFICIALS));
        }
    }

    private void validateAllEntriesFound(
            Set<UUID> requestedIds, List<ApplicationListEntry> entries) {
        Set<UUID> existingIds = new HashSet<>();
        for (ApplicationListEntry entry : entries) {
            existingIds.add(entry.getUuid());
        }

        if (existingIds.size() != requestedIds.size()) {
            Set<UUID> missingIds = new HashSet<>(requestedIds);
            missingIds.removeAll(existingIds);

            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "One or more entries were not found in the source list",
                    java.util.Map.of("invalid_entry_ids", missingIds.toString()));
        }
    }

    private boolean isNullOrEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private boolean isNull(Object value) {
        return value == null;
    }
}
