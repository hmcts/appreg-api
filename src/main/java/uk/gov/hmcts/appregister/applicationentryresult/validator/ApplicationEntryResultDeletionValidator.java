package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * Validator responsible for ensuring that an application list, list entry, and entry result exist
 * and are in a valid state before a delete operation for an entry result is performed.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ApplicationEntryResultDeletionValidator
        implements Validator<ListEntryResultDeleteArgs, ListEntryResultDeleteValidationSuccess> {

    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final AppListEntryResolutionRepository appListEntryResultRepository;

    @Override
    public void validate(ListEntryResultDeleteArgs args) {
        validate(args, (req, success) -> null);
    }

    @Override
    public <R> R validate(
            ListEntryResultDeleteArgs args,
            BiFunction<ListEntryResultDeleteArgs, ListEntryResultDeleteValidationSuccess, R>
                    createSupplier) {
        ApplicationList applicationList =
                applicationListRepository
                        .findByUuid(args.listId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListEntryResultError
                                                        .ENTRY_RESULT_LIST_NOT_FOUND,
                                                "No application list found for UUID '%s'"
                                                        .formatted(args.listId())));

        validateList(applicationList);

        applicationListEntryRepository
                .findActiveByUuidAndApplicationListUuid(args.entryId(), args.listId())
                .orElseThrow(
                        () ->
                                new AppRegistryException(
                                        AppListEntryError.LIST_ENTRY_NOT_FOUND,
                                        ("No application list entry was found for UUID '%s' that belongs to the"
                                                        + " specified list")
                                                .formatted(args.entryId())));

        AppListEntryResolution appListEntryResult =
                appListEntryResultRepository
                        .findByUuidAndApplicationList_Uuid(args.resultId(), args.entryId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListEntryResultError
                                                        .LIST_ENTRY_RESULT_NOT_FOUND,
                                                ("No application list entry result was found for UUID '%s' that"
                                                                + " belongs to the specified entry")
                                                        .formatted(args.resultId())));

        // Build success object and pass it into the caller-supplied function
        ListEntryResultDeleteValidationSuccess success =
                new ListEntryResultDeleteValidationSuccess();
        success.setAppListEntryResult(appListEntryResult);

        return createSupplier.apply(args, success);
    }

    private void validateList(ApplicationList list) {
        if (!list.isOpen()) {
            String msg =
                    "Cannot delete the entry result because the following list is not OPEN: list (uuid=%s)"
                            .formatted(list.getUuid());

            log.warn("List validation failed. {}", msg);

            throw new AppRegistryException(
                    ApplicationListEntryResultError.INVALID_ENTRY_RESULT_LIST_STATUS, msg);
        }
    }
}
