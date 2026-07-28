package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateResults;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

@Component
@Slf4j
@RequiredArgsConstructor
public class BulkApplicationEntryResultCreationValidator
        implements Validator<
                PayloadForCreateResults<BulkResultDto>, BulkApplicationEntryResultCreationSuccess> {

    private final ApplicationListRepository applicationListRepository;

    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final ResolutionCodeRepository resolutionCodeRepository;

    private final BusinessDateProvider businessDateProvider;

    @Override
    public void validate(PayloadForCreateResults<BulkResultDto> validatable) {
        validate(validatable, (v, r) -> null);
    }

    @Override
    public <R> R validate(
            PayloadForCreateResults<BulkResultDto> validatable,
            BiFunction<
                            PayloadForCreateResults<BulkResultDto>,
                            BulkApplicationEntryResultCreationSuccess,
                            R>
                    validateSuccess) {
        var entryIds = getEntryIds(validatable);
        validateDuplicateEntryIds(entryIds);
        validateAndLoadApplicationList(validatable);
        validateEntryIdsProvided(entryIds);
        var entries = loadEntries(validatable, entryIds);
        var createDto = getCreateDto(validatable);
        var resolutionCodeContext = resolveResolutionCodeContext(createDto);
        var bulkSuccess = buildBulkSuccess(entries, entryIds, createDto, resolutionCodeContext);

        // now pass the success details through to the callback so the logic can take place
        return validateSuccess.apply(validatable, bulkSuccess);
    }

    private void validateDuplicateEntryIds(List<UUID> entryIds) {
        if (!entryIds.isEmpty()) {
            validateNoDuplicateEntryIds(entryIds);
        }
    }

    private ApplicationList validateAndLoadApplicationList(
            PayloadForCreateResults<BulkResultDto> validatable) {
        if (validatable.getListId() == null) {
            return null;
        }

        log.debug("Validating bulk result entries for list {}", validatable.getListId());
        var validatedList =
                applicationListRepository
                        .findByUuid(validatable.getListId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListEntryResultError
                                                        .APPLICATION_LIST_DOES_NOT_EXIST,
                                                "The list does not exist %s"
                                                        .formatted(validatable.getListId())));
        validateParentApplicationListIsOpen(validatedList);
        return validatedList;
    }

    private List<ApplicationListEntry> loadEntries(
            PayloadForCreateResults<BulkResultDto> validatable, List<UUID> entryIds) {
        if (validatable.getListId() != null) {
            return loadEntriesForList(validatable.getListId(), entryIds);
        }

        return loadActiveEntries(entryIds);
    }

    private List<ApplicationListEntry> loadEntriesForList(UUID listId, List<UUID> entryIds) {
        var entries =
                applicationListEntryRepository.findByUuidsInSourceList(
                        listId, new HashSet<>(entryIds));
        if (entries.size() >= entryIds.size()) {
            return entries;
        }

        validateMissingEntriesForList(listId, entryIds);
        return entries;
    }

    private void validateMissingEntriesForList(UUID listId, List<UUID> entryIds) {
        var activeEntries = applicationListEntryRepository.findActiveByUuids(entryIds);
        if (activeEntries.size() < entryIds.size()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST,
                    "One or more application list entries do not exist");
        }

        throw new AppRegistryException(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_ENTRIES_NOT_IN_LIST,
                "The bulk result entries are not in the same application list for %s"
                        .formatted(listId));
    }

    private List<ApplicationListEntry> loadActiveEntries(List<UUID> entryIds) {
        var entries = applicationListEntryRepository.findActiveByUuids(entryIds);
        if (entries.size() < entryIds.size()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRIES_NOT_ALL_EXIST,
                    "The entries are not all present");
        }

        entries.forEach(entry -> validateParentApplicationListIsOpen(entry.getApplicationList()));
        return entries;
    }

    private ResultCreateDto getCreateDto(PayloadForCreateResults<BulkResultDto> validatable) {
        return Optional.ofNullable(validatable.getPayload())
                .map(BulkResultDto::getResult)
                .orElse(null);
    }

    private ResolutionCodeContext resolveResolutionCodeContext(ResultCreateDto createDto) {
        if (createDto == null || createDto.getResultCode() == null) {
            return new ResolutionCodeContext(null, null);
        }

        var todayUk = businessDateProvider.currentUkDate();
        var matchingCodes =
                resolutionCodeRepository.findPrioritisingNullEndDate(
                        createDto.getResultCode(), todayUk);
        if (matchingCodes.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST,
                    "No valid resolution code could be found %s"
                            .formatted(createDto.getResultCode()));
        }

        var resolutionCode =
                ReferenceDataSelectionUtil.selectFirstOrderedActiveRecord(
                        matchingCodes,
                        "result code",
                        createDto.getResultCode(),
                        todayUk,
                        ResolutionCode::getEndDate);
        return new ResolutionCodeContext(resolutionCode, resolutionCode.getWording());
    }

    private BulkApplicationEntryResultCreationSuccess buildBulkSuccess(
            List<ApplicationListEntry> entries,
            List<UUID> entryIds,
            ResultCreateDto createDto,
            ResolutionCodeContext resolutionCodeContext) {
        var bulkSuccess = new BulkApplicationEntryResultCreationSuccess();
        var entriesByUuid = indexEntriesByUuid(entries);
        for (var entryId : entryIds) {
            bulkSuccess
                    .getResults()
                    .add(
                            buildValidatedItem(
                                    entriesByUuid, entryId, createDto, resolutionCodeContext));
        }
        return bulkSuccess;
    }

    private Map<UUID, ApplicationListEntry> indexEntriesByUuid(List<ApplicationListEntry> entries) {
        Map<UUID, ApplicationListEntry> entriesByUuid = new HashMap<>();
        for (var entry : entries) {
            entriesByUuid.put(entry.getUuid(), entry);
        }
        return entriesByUuid;
    }

    private BulkApplicationEntryResultValidatedItem buildValidatedItem(
            Map<UUID, ApplicationListEntry> entriesByUuid,
            UUID entryId,
            ResultCreateDto createDto,
            ResolutionCodeContext resolutionCodeContext) {
        var entry = entriesByUuid.get(entryId);
        if (entry == null) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST,
                    "No application list entry exists %s".formatted(entryId));
        }

        return new BulkApplicationEntryResultValidatedItem(
                PayloadForCreateEntryResult.<ResultCreateDto>builder()
                        .listId(entry.getApplicationList().getUuid())
                        .entryId(entry.getUuid())
                        .data(createDto)
                        .build(),
                ListEntryResultCreateValidationSuccess.builder()
                        .applicationList(entry.getApplicationList())
                        .applicationListEntry(entry)
                        .resolutionCode(resolutionCodeContext.resolutionCode())
                        .wordingSentence(toWordingSentence(resolutionCodeContext.wordingTemplate()))
                        .build());
    }

    private WordingTemplateSentence toWordingSentence(String wordingTemplate) {
        return wordingTemplate == null ? null : WordingTemplateSentence.with(wordingTemplate);
    }

    private void validateNoDuplicateEntryIds(List<UUID> entryIds) {
        Set<UUID> uniqueEntryIds = new HashSet<>(entryIds);

        if (uniqueEntryIds.size() != entryIds.size()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE,
                    "Duplicate entry IDs are not allowed");
        }
    }

    private List<UUID> getEntryIds(PayloadForCreateResults<BulkResultDto> validatable) {
        return Optional.ofNullable(validatable.getPayload())
                .map(BulkResultDto::getEntryIds)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    private void validateEntryIdsProvided(List<UUID> entryIds) {
        if (entryIds.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided");
        }
    }

    private void validateParentApplicationListIsOpen(ApplicationList validatable) {
        if (!validatable.isOpen()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list id %s is not in the correct state or the application list is deleted %s"
                            .formatted(validatable.getUuid(), validatable.getStatus()));
        }
    }

    private record ResolutionCodeContext(ResolutionCode resolutionCode, String wordingTemplate) {}
}
