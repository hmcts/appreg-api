package uk.gov.hmcts.appregister.applicationlist.validator;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

/**
 * Validator component for {@link uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto}
 * location fields.
 *
 * <p>Enforces the business rule that exactly one valid location option is provided: either a court
 * location code, or a combination of criminal justice area code and other location description.
 */
@Slf4j
@Component
public class ApplicationUpdateListLocationValidator
        extends AbstractApplicationListLocationValidator<
                PayloadForUpdate<ApplicationListUpdateDto>, ListUpdateValidationSuccess> {

    private AppListEntryResolutionRepository appListEntryResolutionRepository;

    private AppListEntryOfficialRepository appListEntryOfficialRepository;

    private ApplicationListEntryRepository applicationListEntryRepository;

    private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    public ApplicationUpdateListLocationValidator(
            ApplicationListRepository applicationListRepository,
            NationalCourtHouseRepository courtHouseRepository,
            CriminalJusticeAreaRepository criminalJusticeAreaRepository,
            AppListEntryResolutionRepository appListEntryResolutionRepository,
            AppListEntryOfficialRepository appListEntryOfficialRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            AppListEntryFeeStatusRepository appListEntryFeeStatusRepository) {
        super(applicationListRepository, courtHouseRepository, criminalJusticeAreaRepository);
        this.appListEntryResolutionRepository = appListEntryResolutionRepository;
        this.appListEntryOfficialRepository = appListEntryOfficialRepository;
        this.applicationListEntryRepository = applicationListEntryRepository;
        this.appListEntryFeeStatusRepository = appListEntryFeeStatusRepository;
    }

    @Override
    ListUpdateValidationSuccess getResult() {
        return new ListUpdateValidationSuccess();
    }

    @Override
    public void validate(PayloadForUpdate<ApplicationListUpdateDto> dto) {
        validate(dto, null);
    }

    @Override
    public <R> R validate(
            PayloadForUpdate<ApplicationListUpdateDto> dto,
            BiFunction<PayloadForUpdate<ApplicationListUpdateDto>, ListUpdateValidationSuccess, R>
                    createApplicationSupplier) {
        Optional<ApplicationList> applicationListList =
                applicationListRepository.findByUuid(dto.getId());

        // if there is more than one record with the same UUID, it is an error
        if (applicationListList.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.APPLICATION_LIST_NOT_FOUND,
                    "The application list id does not exist : %s".formatted(dto.getId()));
        }

        // validate the status
        validateStatusForUpdate(applicationListList.get());

        // if the list is being closed check if we can close
        if (dto.getData().getStatus() == ApplicationListStatus.CLOSED) {
            validateForClosure(applicationListList.get());
        }

        // validate the location fields
        return super.validate(
                dto,
                (payload, updateLocatedList) -> {
                    updateLocatedList.setApplicationList(applicationListList.get());
                    if (createApplicationSupplier != null) {
                        return createApplicationSupplier.apply(payload, updateLocatedList);
                    }
                    return null;
                });
    }

    /**
     * validate for the closure of the Application list.
     *
     * @param list The list to validate for closure
     */
    private void validateForClosure(ApplicationList list) {
        log.debug("Validating application list with id {} for closure", list.getId());

        // check the duration hours and minutes are not set as they should not be set when closing
        // the list
        if (list.getDurationHours() == 0 && list.getDurationMinutes() == 0) {
            throw new AppRegistryException(
                    ApplicationListError.INVALID_FOR_CLOSE_DURATION,
                    "List cannot be closed. Please add duration hours and/or duration minutes.");
        }

        log.debug("Validated application list duration of payload");

        // gets the application list entries
        List<ApplicationListEntry> listEntries =
                applicationListEntryRepository.findByApplicationListId(list.getId());

        for (ApplicationListEntry listEntry : listEntries) {

            // check each of the resolutions are set for each entry
            List<AppListEntryResolution> listEntryResolutions =
                    appListEntryResolutionRepository.findByApplicationListUuid(listEntry.getUuid());
            if (listEntryResolutions.isEmpty()) {
                throw new AppRegistryException(
                        ApplicationListError.INVALID_FOR_CLOSE_NOT_RESULTED,
                        "List cannot be closed. Please result all the applications in the list and try again");
            }

            log.debug("Validated application entry resolution with entry id {}", listEntry.getId());

            // make sure the officials are set for each entry
            List<AppListEntryOfficial> listEntryOfficials =
                    appListEntryOfficialRepository.getOfficialByEntryUuid(listEntry.getUuid());
            if (listEntryOfficials.isEmpty()) {
                throw new AppRegistryException(
                        ApplicationListError.INVALID_FOR_CLOSE_NO_OFFICIAL,
                        "List cannot be closed. No Official is recorded against any of the applications in the list.");
            }

            log.debug("Validated application entry officials with entry id {}", listEntry.getId());

            validStatusIsPaid(listEntry);
        }

        log.debug("Validated application list with id {} for closure", list.getId());
    }

    /**
     * validate the app list entry has a paid status.
     *
     * @param listEntry The list entry to validate if it has been paid.
     */
    private void validStatusIsPaid(ApplicationListEntry listEntry) {
        // make sure the fee status has been paid
        YesOrNo yesOrNo = listEntry.getApplicationCode().getFeeDue();
        if (yesOrNo.isYes()) {
            List<AppListEntryFeeStatus> listStatuses =
                    appListEntryFeeStatusRepository.findByAppListEntryId(listEntry.getId());

            // determine if one of the fee statuses has been paid
            boolean paid = false;
            for (AppListEntryFeeStatus status : listStatuses) {
                if (status.getAlefsFeeStatus() == FeeStatusType.PAID) {
                    paid = true;
                }
            }

            // if the entry is not paid then we can not close the list
            if (!paid) {
                throw new AppRegistryException(
                        ApplicationListError.INVALID_FOR_CLOSE_NOT_PAID,
                        "List cannot be closed. All entries do not have a Paid status.");
            }

            log.debug("Validated application entry fee status with entry id {}", listEntry.getId());
        }
    }

    /**
     * Validates we can not reopen a closed record.
     *
     * @param currentList The current record data
     */
    private void validateStatusForUpdate(ApplicationList currentList) {
        // fail any update on an already closed list
        if (currentList.getStatus() == Status.CLOSED) {
            throw new AppRegistryException(
                    ApplicationListError.INVALID_LIST_STATUS,
                    "A closed application list is not allowed to be updated");
        }
    }

    @Override
    Function<PayloadForUpdate<ApplicationListUpdateDto>, String> getCourtLocation() {
        return (dto) ->
                dto.getData().getCourtLocationCode() != null
                        ? dto.getData().getCourtLocationCode()
                        : null;
    }

    @Override
    Function<PayloadForUpdate<ApplicationListUpdateDto>, String> getCjaCode() {
        return (dto) -> dto.getData().getCjaCode() != null ? dto.getData().getCjaCode() : null;
    }

    @Override
    Function<PayloadForUpdate<ApplicationListUpdateDto>, String> getLocationDescription() {
        return (dto) -> dto.getData().getOtherLocationDescription();
    }

    @Override
    Function<PayloadForUpdate<ApplicationListUpdateDto>, ApplicationListStatus> getStatus() {
        return (dto) -> dto.getData().getStatus();
    }

    @Override
    Function<PayloadForUpdate<ApplicationListUpdateDto>, LocalTime> getTime() {
        return (dto) -> dto.getData().getTime();
    }
}
