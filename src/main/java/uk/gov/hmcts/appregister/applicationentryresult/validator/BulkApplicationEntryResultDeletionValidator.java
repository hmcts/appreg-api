package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultItemDto;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultsDto;

@Component
@RequiredArgsConstructor
public class BulkApplicationEntryResultDeletionValidator
        implements Validator<BulkDeleteResultsDto, BulkApplicationEntryResultDeletionSuccess> {

    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final AppListEntryResolutionRepository appListEntryResolutionRepository;

    @Override
    public void validate(BulkDeleteResultsDto validatable) {
        validate(validatable, (request, success) -> null);
    }

    @Override
    public <R> R validate(
            BulkDeleteResultsDto validatable,
            BiFunction<BulkDeleteResultsDto, BulkApplicationEntryResultDeletionSuccess, R>
                    validateSuccess) {
        var results =
                Optional.ofNullable(validatable)
                        .map(BulkDeleteResultsDto::getResults)
                        .map(ArrayList::new)
                        .orElseGet(ArrayList::new);

        if (results.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED, "No result delete items provided");
        }

        validateNoDuplicateItems(results);

        var listIds = collectDistinct(results, BulkDeleteResultItemDto::getListId);
        var entryIds = collectDistinct(results, BulkDeleteResultItemDto::getEntryId);
        var resultIds = collectDistinct(results, BulkDeleteResultItemDto::getResultId);

        var listsByUuid =
                applicationListRepository.findByUuidIncludingDeleteIn(listIds).stream()
                        .collect(Collectors.toMap(ApplicationList::getUuid, Function.identity()));
        var entriesByUuid =
                applicationListEntryRepository.findByUuidIncludingDeleteIn(entryIds).stream()
                        .collect(
                                Collectors.toMap(
                                        ApplicationListEntry::getUuid, Function.identity()));
        var activeEntriesByKey =
                applicationListEntryRepository
                        .findActiveByUuidsAndApplicationListUuids(entryIds, listIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        entry ->
                                                new EntryToList(
                                                        entry.getUuid(),
                                                        entry.getApplicationList().getUuid()),
                                        Function.identity()));
        var resultsByUuid =
                appListEntryResolutionRepository.findByUuidIn(resultIds).stream()
                        .collect(
                                Collectors.toMap(
                                        AppListEntryResolution::getUuid, Function.identity()));

        var success = new BulkApplicationEntryResultDeletionSuccess();
        for (var result : results) {
            var args =
                    new ListEntryResultDeleteArgs(
                            result.getListId(), result.getEntryId(), result.getResultId());
            var applicationList = listsByUuid.get(args.listId());
            if (applicationList == null) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST,
                        "The application list does not exist %s".formatted(args.listId()));
            }
            if (applicationList.isDeleted() || !applicationList.isOpen()) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT,
                        "The application list id %s is not in the correct state or the application list is deleted %s"
                                .formatted(args.listId(), applicationList.getStatus()));
            }

            if (!entriesByUuid.containsKey(args.entryId())) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST,
                        "No application list entry exists %s".formatted(args.entryId()));
            }

            var activeEntryKey = new EntryToList(args.entryId(), args.listId());
            var applicationListEntry = activeEntriesByKey.get(activeEntryKey);
            if (applicationListEntry == null) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_ENTRY_NOT_WITHIN_LIST,
                        "The application list entry %s does not belong to list %s"
                                .formatted(args.entryId(), args.listId()));
            }

            var appListEntryResult = resultsByUuid.get(args.resultId());
            if (appListEntryResult == null) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST,
                        "No application list entry result was found for UUID '%s'"
                                .formatted(args.resultId()));
            }

            if (!args.entryId().equals(appListEntryResult.getApplicationList().getUuid())) {
                throw new AppRegistryException(
                        ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_NOT_WITHIN_ENTRY,
                        ("No application list entry result was found for UUID '%s' that"
                                        + " belongs to the specified entry")
                                .formatted(args.resultId()));
            }

            success.getResults()
                    .add(
                            new BulkApplicationEntryResultDeletionValidatedItem(
                                    args,
                                    new ListEntryResultDeleteValidationSuccess(
                                            null,
                                            null,
                                            applicationList,
                                            applicationListEntry,
                                            appListEntryResult)));
        }

        return validateSuccess.apply(validatable, success);
    }

    private List<UUID> collectDistinct(
            List<BulkDeleteResultItemDto> results, Function<BulkDeleteResultItemDto, UUID> mapper) {
        return results.stream().map(mapper).distinct().toList();
    }

    private void validateNoDuplicateItems(List<BulkDeleteResultItemDto> results) {
        Set<String> uniqueKeys = new HashSet<>();
        for (var result : results) {
            var key =
                    "%s|%s|%s"
                            .formatted(
                                    result.getListId(), result.getEntryId(), result.getResultId());
            if (!uniqueKeys.add(key)) {
                throw new AppRegistryException(
                        ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE,
                        "Duplicate result delete items are not allowed");
            }
        }
    }
}
