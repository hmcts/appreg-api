package uk.gov.hmcts.appregister.applicationlist.validator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
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
        validate(dto, (req, success) -> null);
    }

    @Override
    public <R> R validate(
        MoveEntriesDto dto,
        BiFunction<MoveEntriesDto, MoveEntriesValidationSuccess, R> createSupplier) {
        ApplicationList sourceList = applicationListRepository.findByUuid(sourceListId)
            .orElseThrow(() -> new AppRegistryException(
                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                "No source application list found for UUID '%s'".formatted(sourceListId)));

        ApplicationList targetList = applicationListRepository.findByUuid(dto.getTargetListId())
            .orElseThrow(() -> new AppRegistryException(
                ApplicationListError.TARGET_LIST_NOT_FOUND,
                "No target application list found for UUID '%s'".formatted(dto.getTargetListId())));

        validateLists(sourceList, targetList);

        if (dto.getEntryIds() == null || dto.getEntryIds().isEmpty()) {
            throw new AppRegistryException(ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided");
        }

        Set<UUID> requestedIds = new HashSet<>(dto.getEntryIds());
        long foundCount = applicationListEntryRepository
            .countByUuidInAndApplicationListUuid(requestedIds, sourceListId);

        if (foundCount != requestedIds.size()) {
            throw new AppRegistryException(
                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                "One or more entries were not found in the source list");
        }

        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setTargetList(targetList);

        return createSupplier.apply(dto, success);
    }

    private void validateLists(ApplicationList sourceList, ApplicationList targetList) {
        boolean sourceNotOpen = !sourceList.isOpen();
        boolean targetNotOpen = !targetList.isOpen();

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
