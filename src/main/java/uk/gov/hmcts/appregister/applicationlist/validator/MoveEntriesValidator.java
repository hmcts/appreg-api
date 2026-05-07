package uk.gov.hmcts.appregister.applicationlist.validator;

import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.model.MoveEntriesPayload;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoveEntriesValidator
        implements Validator<MoveEntriesPayload, MoveEntriesValidationSuccess> {

    private final ApplicationListRepository applicationListRepository;

    @Override
    public void validate(MoveEntriesPayload payload) {
        validate(payload, (req, success) -> null);
    }

    @Override
    public <R> R validate(
            MoveEntriesPayload payload,
            BiFunction<MoveEntriesPayload, MoveEntriesValidationSuccess, R> createSupplier) {
        var sourceListId = payload.sourceListId();
        var moveEntriesDto = payload.moveEntriesDto();

        ApplicationList sourceList =
                applicationListRepository
                        .findByUuid(sourceListId)
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                                "No source application list found for UUID '%s'"
                                                        .formatted(sourceListId)));

        ApplicationList targetList =
                applicationListRepository
                        .findByUuid(moveEntriesDto.getTargetListId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListError.TARGET_LIST_NOT_FOUND,
                                                "No target application list found for UUID '%s'"
                                                        .formatted(
                                                                moveEntriesDto.getTargetListId())));

        validateLists(sourceList, targetList);

        if (moveEntriesDto.getEntryIds() == null || moveEntriesDto.getEntryIds().isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided");
        }

        var success = new MoveEntriesValidationSuccess();
        success.setTargetList(targetList);

        return createSupplier.apply(payload, success);
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
