package uk.gov.hmcts.appregister.applicationentry.validator;

import static uk.gov.hmcts.appregister.generated.model.PaymentStatus.DUE;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.applicationcode.enumeration.ApplicationCodeTypeEnum;
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
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Respondent;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApplicationEntryValidator<T, O> implements Validator<T, O> {
    private final ApplicationListRepository applicationListRepository;
    private final ApplicationCodeRepository applicationCodeRepository;
    private final FeeRepository feeRepository;
    private final Clock clock;
    private final StandardApplicantRepository standardApplicantRepository;

    private static final String BULK_RESPONDENT_NOT_REQUIRED_MESSAGE =
            "Bulk respondent not required for code %s";

    public void validate(T validatable) {
        validate(validatable, null);
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
    public <R> R validate(T validatable, BiFunction<T, O, R> validateSuccess) {

        // ensure mutual exclusivity of the respondent
        ensureRespondentMutualExclusion(validatable);

        // ensure mutual exclusivity of the applicant
        ensureApplicantMutualExclusion(validatable);

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

        if (validateSuccess != null) {
            return validateSuccess.apply(
                    validatable,
                    getResult(
                            code,
                            wordingTemplateCollection,
                            fee,
                            saCode,
                            applicationList,
                            validatable));
        }
        return null;
    }

    /**
     * gets the result of the validation.
     *
     * @param code The application code
     * @param wordingTemplateCollection The wording template collection
     * @param fee The fee
     * @param saCode The standard applicant code
     * @param applicationList The application list
     */
    protected abstract O getResult(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            Fee fee,
            StandardApplicant saCode,
            ApplicationList applicationList,
            T dto);

    /**
     * validate the application list for the app list entry creation. Validates that the
     * aapplication list exists
     *
     * @param validatable The validatable payload
     * @return The application list if found
     */
    private ApplicationList validateParentApplicationList(T validatable) {
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(
                        getApplicationListUuid(validatable));
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "The application list does not exist %s"
                            .formatted(getApplicationListUuid(validatable)));
        }

        // if the state of the application is not open then we cant add an entry
        if (applicationList.get().getStatus() != Status.OPEN || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list id %s is not in the correct state or the application list is deleted %s"
                            .formatted(
                                    getApplicationListUuid(validatable),
                                    applicationList.get().getStatus()));
        }

        log.debug("Validated application list {}", getApplicationListUuid(validatable));

        return applicationList.get();
    }

    /**
     * validate the standard applicant code exists for the applicant.
     *
     * @param validatable The dto payload to validate
     * @return The standard applicant or null if not applicable
     */
    private StandardApplicant validateStandardApplicantCode(T validatable) {
        String standardApplicantCode = getStandardApplicantCode(validatable);

        // validate the standard applicant code if provided
        List<StandardApplicant> saCode;
        if (standardApplicantCode != null) {
            saCode =
                    standardApplicantRepository.findStandardApplicantByCodeAndDate(
                            standardApplicantCode, LocalDate.now(clock));

            if (saCode.size() > 1) {
                throw new AppRegistryException(
                        AppListEntryError.MULTIPLE_STANDARD_APPLICANT_EXIST,
                        "Multiple standard applicant codes exist for %s"
                                .formatted(standardApplicantCode));
            }

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
    private void ensureApplicantMutualExclusion(T dto) {
        boolean hasOrganisation =
                getApplicant(dto) != null && getApplicant(dto).getOrganisation() != null;

        boolean hasPerson = getApplicant(dto) != null && getApplicant(dto).getPerson() != null;

        boolean hasCode = getStandardApplicantCode(dto) != null;

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
                            .formatted(getApplicant(dto)));
        }

        log.debug("Validated mutual exclusivity of applicant {}", getApplicant(dto));
    }

    /**
     * gets the respondent organisation.
     *
     * @param validatable The validatable payload
     * @return The organisation respondent
     */
    protected abstract Respondent getRespondent(T validatable);

    /**
     * gets the respondent organisation.
     *
     * @param validatable The validatable payload
     * @return The organisation respondent
     */
    protected abstract Applicant getApplicant(T validatable);

    /**
     * gets the application code.
     *
     * @param validatable The validatable payload
     * @return The application code
     */
    protected abstract String getApplicationCode(T validatable);

    /**
     * gets the fee statuses.
     *
     * @param validatable The validatable payload
     * @return The fee statuses
     */
    protected abstract List<FeeStatus> getFeeStatuses(T validatable);

    /**
     * has an offsite fee.
     *
     * @param validatable The validatable payload
     * @return The offsite fee
     */
    protected abstract Boolean getHasOffsiteFee(T validatable);

    /**
     * get app list.
     *
     * @param validatable The validatable payload
     * @return The app list id
     */
    protected abstract UUID getApplicationListUuid(T validatable);

    /**
     * get standard code.
     *
     * @param validatable The validatable payload
     * @return The standard code
     */
    protected abstract String getStandardApplicantCode(T validatable);

    /**
     * get number of respondents.
     *
     * @param validatable The validatable payload
     * @return The number of respondents
     */
    protected abstract Integer getNumberOfRespondents(T validatable);

    /**
     * get account number.
     *
     * @param validatable The validatable payload
     * @return The account number
     */
    protected abstract String getAccountNumber(T validatable);

    /**
     * validate the respondent of the payload and ensures mutual exclusivity between the
     * organisation and person.
     *
     * @param dto The dto to validate
     */
    private void ensureRespondentMutualExclusion(T dto) {
        if (getRespondent(dto) != null) {
            if (!(getRespondent(dto) != null && getRespondent(dto).getOrganisation() != null)
                    ^ (getRespondent(dto) != null && getRespondent(dto).getPerson() != null)) {
                throw new AppRegistryException(
                        AppListEntryError.RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON,
                        "The respondent type can only be an organsisation or person %s"
                                .formatted(getRespondent(dto)));
            }
        }

        log.debug("Validated mutual exclusivity of respondent {}", getRespondent(dto));
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
    private ApplicationCode validateApplicationCode(T validatable) {
        if (getApplicationCode(validatable) != null
                && ApplicationCodeTypeEnum.isMatching(
                        ApplicationCodeTypeEnum.ENFORCEMENT_FINES,
                        getApplicationCode(validatable))) {
            // if the account number is null or empty then throw an error as we require
            // an account number for enforcement fines codes
            if (getAccountNumber(validatable) == null || getAccountNumber(validatable).isEmpty()) {
                throw new AppRegistryException(
                        AppListEntryError.ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE,
                        "Application number required for application code %s"
                                .formatted(getApplicationCode(validatable)));
            }
        }

        // validate that the application code exists and is valid for today
        List<ApplicationCode> code =
                applicationCodeRepository.findByCodeAndDate(
                        getApplicationCode(validatable), LocalDate.now(clock));

        if (code.size() > 1) {
            throw new AppRegistryException(
                    AppListEntryError.MULTIPLE_APPLICATION_CODE_EXIST,
                    "Multiple application codes exist for %s"
                            .formatted(getApplicationCode(validatable)));
        }

        if (code.size() == 0) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST,
                    "No valid code can be found %s".formatted(getApplicationCode(validatable)));
        }

        log.debug("Validated the application code {}", getApplicationCode(validatable));
        return code.getFirst();
    }

    /**
     * validate the code and the fees details provided in the payload. If the code requires a fee
     * then one must be provided else an exception is thrown
     *
     * <p>In addition: - For each fee status, if paymentStatus = DUE then paymentReference must NOT
     * be provided.
     *
     * @param validatable The validatable payload
     */
    private Fee validateFee(ApplicationCode applicationCode, T validatable) {
        Fee feeToReturn = null;

        // gets the fee statuses from the payload or an empty list if none provided
        List<FeeStatus> feeStatuses =
                getFeeStatuses(validatable) == null ? List.of() : getFeeStatuses(validatable);

        validatePaymentReferenceNotAllowedWhenDue(feeStatuses, validatable);

        // check that the fee status payload make sense according to the application code
        YesOrNo yesOrNo = applicationCode.getFeeDue();
        if (yesOrNo == YesOrNo.YES && feeStatuses.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_REQUIRED,
                    "Fee required for code %s".formatted(getApplicationCode(validatable)));
        } else if (yesOrNo == YesOrNo.NO && !feeStatuses.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.FEE_NOT_REQUIRED,
                    "Fee is provided but not required for code %s"
                            .formatted(getApplicationCode(validatable)));
        }

        // if the fee is required but it cant be found then error
        if (applicationCode.getFeeDue() == YesOrNo.YES) {
            List<Fee> fees =
                    feeRepository.findByReferenceBetweenDateWithOffsite(
                            applicationCode.getFeeReference(),
                            LocalDate.now(clock),
                            getHasOffsiteFee(validatable) != null && getHasOffsiteFee(validatable));

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

    /** Validates that when payment status is DUE, no payment reference is provided. */
    private void validatePaymentReferenceNotAllowedWhenDue(
            List<FeeStatus> feeStatuses, T validatable) {

        if (feeStatuses == null || feeStatuses.isEmpty()) {
            return;
        }

        for (FeeStatus feeStatus : feeStatuses) {
            if (feeStatus == null) {
                continue;
            }

            boolean isDue = feeStatus.getPaymentStatus() == DUE;

            String paymentReference = feeStatus.getPaymentReference();
            boolean paymentReferencePassed =
                    paymentReference != null && !paymentReference.trim().isEmpty();

            if (isDue && paymentReferencePassed) {
                throw new AppRegistryException(
                        AppListEntryError.PAYMENT_REFERENCE_NOT_ALLOWED_WHEN_PAYMENT_DUE,
                        "Payment reference must not be provided when fee status is DUE for code %s"
                                .formatted(getApplicationCode(validatable)));
            }
        }
    }

    /**
     * validate the respondent details provided in the payload. This includes checking whether the
     * application code requires a respondent and whether bulk respondents are allowed
     *
     * @param applicationCode The application code
     * @param validatable The validatable payload
     */
    private void validateRespondent(ApplicationCode applicationCode, T validatable) {

        // if respondent is required, check that it exists in the payload
        if (applicationCode.getRequiresRespondent() == YesOrNo.YES
                && getRespondent(validatable) == null) {
            throw new AppRegistryException(
                    AppListEntryError.RESPONDENT_REQUIRED,
                    "Respondent required for code %s".formatted(getApplicationCode(validatable)));
        }

        // check bulk respondent is off and no respondents are specified in the payload
        if (applicationCode.getBulkRespondentAllowed() == YesOrNo.NO
                && getNumberOfRespondents(validatable) != null
                && getNumberOfRespondents(validatable) != 0) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED,
                    BULK_RESPONDENT_NOT_REQUIRED_MESSAGE.formatted(
                            getApplicationCode(validatable)));
        }

        // if we are setting multiple respondents, check that the application code allows it
        if (applicationCode.getBulkRespondentAllowed() == YesOrNo.NO
                && getRespondent(validatable) != null
                && (getNumberOfRespondents(validatable) != null
                        && getNumberOfRespondents(validatable) != 0)) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED,
                    BULK_RESPONDENT_NOT_REQUIRED_MESSAGE.formatted(
                            getApplicationCode(validatable)));
        }

        // if we are setting multiple respondents, check that the application code allows it
        if (applicationCode.getRequiresRespondent() == YesOrNo.YES
                        && getRespondent(validatable) == null
                || (getNumberOfRespondents(validatable) != null
                        && getNumberOfRespondents(validatable) == 0)) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_RESPONDENT_NOT_EXPECTED,
                    "Bulk respondent not required for code %s"
                            .formatted(getApplicationCode(validatable)));
        }

        log.debug("Validated the respondent details");
    }
}
