package uk.gov.hmcts.appregister.applicationlist.validator;

import static uk.gov.hmcts.appregister.generated.model.ApplicationListStatus.OPEN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoveEntriesValidator
        implements Validator<MoveEntriesDto, MoveEntriesValidationSuccess> {

    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;

    // Injected ad-hoc per validation call
    @Setter private UUID sourceListId;

    public MoveEntriesValidator withSourceList(UUID sourceListId) {
        this.sourceListId = sourceListId;
        return this;
    }

    @Override
    public void validate(MoveEntriesDto dto) {
        validate(dto, null);
    }

    @Override
    public <R> R validate(
            MoveEntriesDto dto,
            BiFunction<MoveEntriesDto, MoveEntriesValidationSuccess, R> createSupplier) {

        if (sourceListId == null) {
            throw new IllegalStateException("sourceListId must be provided before validation");
        }

        // Load source list
        ApplicationList sourceList =
                applicationListRepository
                        .findByUuid(sourceListId)
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                                "No source application list found for UUID '%s'"
                                                        .formatted(sourceListId)));

        // Load target list
        ApplicationList targetList =
                applicationListRepository
                        .findByUuid(dto.getTargetListId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListError.TARGET_LIST_NOT_FOUND,
                                                "No target application list found for UUID '%s'"
                                                        .formatted(dto.getTargetListId())));

        validateLists(sourceList, targetList);

        log.debug(
                "List validation successful. Source list (uuid={}), target list (uuid={}) are both OPEN.",
                sourceList.getUuid(),
                targetList.getUuid());

        if (dto.getEntryIds() == null || dto.getEntryIds().isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided");
        }

        Set<UUID> requestedIds = new HashSet<>(dto.getEntryIds());
        List<ApplicationListEntry> loadedEntries =
                applicationListEntryRepository.findAllByUuidIn(requestedIds);

        Map<UUID, ApplicationListEntry> loadedByUuid =
                loadedEntries.stream()
                        .collect(Collectors.toMap(ApplicationListEntry::getUuid, e -> e));

        List<ApplicationListEntry> toSave = new ArrayList<>();

        for (UUID id : requestedIds) {
            ApplicationListEntry entry = loadedByUuid.get(id);
            if (entry == null) {
                throw new AppRegistryException(
                        ApplicationListError.ENTRY_NOT_FOUND,
                        "No application list entry found for UUID '%s'".formatted(id));
            }

            if (!sourceListId.equals(entry.getApplicationList().getUuid())) {
                throw new AppRegistryException(
                        ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                        "Application list entry '%s' does not belong to the source list"
                                .formatted(id));
            }

            entry.setApplicationList(targetList);
            toSave.add(entry);
        }

        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setSourceList(sourceList);
        success.setTargetList(targetList);
        success.setEntriesToSave(toSave);

        if (createSupplier != null) {
            return createSupplier.apply(dto, success);
        }

        return null;
    }

    private void validateLists(ApplicationList sourceList, ApplicationList targetList) {
        boolean sourceNotOpen = sourceList.getStatus() != OPEN;
        boolean targetNotOpen = targetList.getStatus() != OPEN;

        if (sourceNotOpen || targetNotOpen) {
            StringBuilder msg =
                    new StringBuilder(
                            "Cannot move the applications because the following lists are not OPEN: ");

            if (sourceNotOpen) {
                msg.append(String.format("source list (uuid=%s) ", sourceList.getUuid()));
            }
            if (targetNotOpen) {
                msg.append(String.format("target list (uuid=%s) ", targetList.getUuid()));
            }

            log.warn("List validation failed. {}", msg.toString().trim());

            throw new AppRegistryException(
                    ApplicationListError.INVALID_LIST_STATUS, msg.toString().trim());
        }
    }
}
