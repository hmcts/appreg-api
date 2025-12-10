package uk.gov.hmcts.appregister.applicationentry.validator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

/**
 * Validates the dto for an application entry create.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateApplicationEntryValidator
        implements Validator<
                PayloadForCreate<EntryCreateDto>, CreateApplicationEntryValidationSuccess> {
    private final ApplicationListRepository applicationListRepository;
    private final ApplicationCodeRepository applicationCodeRepository;
    private final FeeRepository feeRepository;
    private final Clock clock;
    private final StandardApplicantRepository standardApplicantRepository;

    @Override
    public void validate(PayloadForCreate<EntryCreateDto> validatable) {
        validate(validatable, (v, r) -> null);
    }

    /**
     * This validator has many rules. The rules are as such:- - Applicants are mutually exclusive in
     * terms of organisation, person or standard applicant - Respondents are mutually exclusive in
     * terms of organisation or person. Respondent is only required if the application code requires
     * it. - The application list must exist and be in the open state - The application code must
     * exist and be valid for today - If the application code requires a fee then one must be
     * provided - If the application code does not require a fee then none must be provided - If the
     * application code requires a respondent then one must be provided - If the application code
     * does not require a respondent then none must be provided - If the application code does not
     * allow bulk respondents then none must be provided - If the fee is provided then it must exist
     * - If the standard applicant code is provided then it must exist - If bulk respondents are
     * provided then the application code must allow it - The code fee must exist in order to map to
     * the new application entry
     *
     * @param validatable The validatable payload
     * @param validateSuccess The success function to call if validation is successful
     */
    @Override
    public <R> R validate(
            PayloadForCreate<EntryCreateDto> validatable,
            BiFunction<PayloadForCreate<EntryCreateDto>, CreateApplicationEntryValidationSuccess, R>
                    validateSuccess) {

        // ensure mutual exclusivity of the respondent
        ensureRespondentMutualExclusion(validatable.getData());

        // ensure mutual exclusivity of the applicant
        ensureApplicantMutualExclusion(validatable.getData());

        ApplicationList applicationList = validateParentApplicationList(validatable);

        StandardApplicant saCode = validateStandardApplicantCode(validatable);

        // validate that the application code exists and is valid for today
        ApplicationCode code = validateApplicationCode(validatable);

        // parse the wording template and error if not valid
        WordingTemplateSentence wordingTemplateCollection =
                WordingTemplateSentence.with(code.getWording());

        // if fee is due get the fee
        Fee fee = validateFee(code, validatable);

        // validate the respondent if required
        validateRespondent(code, validatable);

        // build the success response so the calling logic can use the acquired entities
        CreateApplicationEntryValidationSuccess validateSuccessResponse =
                CreateApplicationEntryValidationSuccess.builder()
                        .applicationCode(code)
                        .wordingSentence(wordingTemplateCollection)
                        .fee(fee)
                        .sa(saCode)
                        .applicationList(applicationList)
                        .build();
        return validateSuccess.apply(validatable, validateSuccessResponse);
    }

    /**
     * validate the application list for the app list entry creation. Validates that the
     * aapplication list exists
     *
     * @param validatable The validatable payload
     * @return The application list if found
     */
    private ApplicationList validateParentApplicationList(
            PayloadForCreate<EntryCreateDto> validatable) {
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(validatable.getId());
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "The application list does not exist %s".formatted(validatable.getId()));
        }

        // if the state of the application is not open then we cant add an entry
        if (applicationList.get().getStatus() != Status.OPEN || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE,
                    "The application list id %s is not in the correct state or the application list is deleted %s"
                            .formatted(validatable.getId(), applicationList.get().getStatus()));
        }

        log.debug("Validated application list {}", validatable.getId());

        return applicationList.get();
    }

    /**
     * validate the standard applicant code exists for the applicant.
     *
     * @param validatable The dto payload to validate
     * @return The standard applicant or null if not applicable
     */
    private StandardApplicant validateStandardApplicantCode(
            PayloadForCreate<EntryCreateDto> validatable) {
        String standardApplicantCode = validatable.getData().getStandardApplicantCode();

        // validate the standard applicant code if provided
        List<StandardApplicant> saCode;
        if (standardApplicantCode != null) {
            saCode =
                    standardApplicantRepository.findStandardApplicantByCodeAndDate(
                            standardApplicantCode, LocalDate.now(clock));

            if (saCode.isEmpty()) {
                // throw exception we expect a valid standard applicant code
                throw new AppRegistryException(
                        AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST,
                        "The standard applicant does not exist %s"
                                .formatted(standardApplicantCode));
            }

            log.debug("Validated standard applicant {}", standardApplicantCode);

            return saCode.getFirst();
        }

        return null;
    }

    /**
     * validate the applicant of the payload and ensures mutual exclusivity.
     *
     * @param dto The dto to validate
     */
    private void ensureApplicantMutualExclusion(EntryCreateDto dto) {
        boolean hasOrganisation =
                dto.getApplicant() != null && dto.getApplicant().getOrganisation() != null;

        boolean hasPerson = dto.getApplicant() != null && dto.getApplicant().getPerson() != null;

        boolean hasCode = dto.getStandardApplicantCode() != null;

        int count = 0;
        if (hasOrganisation) {
            count++;
        }

        if (hasPerson) {
            count++;
        }

        if (hasCode) {
            count++;
        }

        if (count != 1) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON,
                    "The applicant type can only be an organisation or person %s"
                            .formatted(dto.getApplicant()));
        }

        log.debug("Validated mutual exclusivity of applicant {}", dto.getApplicant());
    }

    /**
     * validate the respondent of the payload and ensures mutual exclusivity between the
     * organisation and person.
     *
     * @param dto The dto to validate
     */
    private void ensureRespondentMutualExclusion(EntryCreateDto dto) {
        if (dto.getRespondent() != null) {
            if (!(dto.getRespondent() != null && dto.getRespondent().getOrganisation() != null)
                    ^ (dto.getRespondent() != null && dto.getRespondent().getPerson() != null)) {
                throw new AppRegistryException(
                        AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON,
                        "The respondent type can only be an organsisation or person %s"
                                .formatted(dto.getRespondent()));
            }
        }

        log.debug("Validated mutual exclusivity of respondent {}", dto.getRespondent());
    }

    /**
     * validate the provided code against the payload details. Check to determine if the code is
     * actually valid for today. Also whether the payload should include a fee, respondent and bulk
     * respondents
     *
     * @param validatable The dto to validate
     * @return The code if validate
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException In the event of a
     *     failure
     */
    private ApplicationCode validateApplicationCode(PayloadForCreate<EntryCreateDto> validatable) {
        // validate that the application code exists and is valid for today
        List<ApplicationCode> code =
                applicationCodeRepository.findByCodeAndDate(
                        validatable.getData().getApplicationCode(), LocalDate.now(clock));
        if (code.size() == 0) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICANT_CODE_DOES_NOT_EXIST,
                    "No valid code can be found %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        log.debug("Validated the application code {}", validatable.getData().getApplicationCode());
        return code.getFirst();
    }

    /**
     * validate the code and the fees details provided in the payload. If the code requires a fee
     * then one must be provided else an exception is thrown
     *
     * @param validatable The validatable payload
     */
    private Fee validateFee(
            ApplicationCode applicationCode, PayloadForCreate<EntryCreateDto> validatable) {
        Fee feeToReturn = null;

        // check that the fee status payload make sense according to the application code
        YesOrNo yesOrNo = applicationCode.getFeeDue();
        if (yesOrNo == YesOrNo.YES && validatable.getData().getFeeStatuses().isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_REQUIRED,
                    "Fee required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        } else if (yesOrNo == YesOrNo.NO
                && validatable.getData().getFeeStatuses() != null
                && !validatable.getData().getFeeStatuses().isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_NOT_REQUIRED,
                    "Fee is provided but not required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        // if the fee is required but it cant be found then error
        if (applicationCode.getFeeDue() == YesOrNo.YES) {
            List<Fee> fees =
                    feeRepository.findByReferenceBetweenDateWithOffsite(
                            applicationCode.getFeeReference(),
                            LocalDate.now(clock),
                            validatable.getData().getHasOffsiteFee() != null
                                    && validatable.getData().getHasOffsiteFee());

            if (fees.isEmpty()) {
                // throw an exception as we have no feeds
                throw new AppRegistryException(
                        AppListEntryError.FEE_OFFSITE_NOT_SUITABLE,
                        "Fee offsite does not exist for code %s"
                                .formatted(applicationCode.getCode()));
            }

            feeToReturn = fees.getFirst();
            log.debug("Validated the fee {}", feeToReturn.getId());
        }

        return feeToReturn;
    }

    /**
     * validate the respondent details provided in the payload. This includes checking whether the
     * application code requires a respondent and whether bulk respondents are allowed
     *
     * @param applicationCode The application code
     * @param validatable The validatable payload
     */
    private void validateRespondent(
            ApplicationCode applicationCode, PayloadForCreate<EntryCreateDto> validatable) {

        // if respondent is required, check that it exists in the payload
        if (applicationCode.getRequiresRespondent() == YesOrNo.YES
                && validatable.getData().getRespondent() == null) {
            throw new AppRegistryException(
                    AppListEntryError.RESPONDENT_REQUIRED,
                    "Respondent required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        // check bulk respondent is off and no respondents are specified in the payload
        if (applicationCode.getBulkRespondentAllowed() == YesOrNo.NO
                && validatable.getData().getNumberOfRespondents() != null
                && validatable.getData().getNumberOfRespondents() != 0) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED,
                    "Bulk respondent not required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        // if we do not require a respondent, check that none exists in the payload
        if (applicationCode.getRequiresRespondent() == YesOrNo.NO
                && validatable.getData().getRespondent() != null) {
            throw new AppRegistryException(
                    AppListEntryError.NOT_RESPONDENT_REQUIRED,
                    "Respondent not required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        // if we are setting multiple respondents, check that the application code allows it
        if (applicationCode.getBulkRespondentAllowed() == YesOrNo.NO
                && validatable.getData().getRespondent() != null
                && (validatable.getData().getNumberOfRespondents() != null
                        && validatable.getData().getNumberOfRespondents() != 0)) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED,
                    "Bulk respondent not required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        // if we are setting multiple respondents, check that the application code allows it
        if (applicationCode.getRequiresRespondent() == YesOrNo.YES
                        && validatable.getData().getRespondent() == null
                || (validatable.getData().getNumberOfRespondents() != null
                        && validatable.getData().getNumberOfRespondents() == 0)) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED,
                    "Bulk respondent not required for code %s"
                            .formatted(validatable.getData().getApplicationCode()));
        }

        log.debug("Validated the respondent details");
    }
}
