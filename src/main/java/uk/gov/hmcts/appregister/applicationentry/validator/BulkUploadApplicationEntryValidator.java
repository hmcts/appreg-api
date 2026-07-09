package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationcode.enumeration.ApplicationCodeTypeEnum;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;

@Component
@Slf4j
public class BulkUploadApplicationEntryValidator extends CreateApplicationEntryValidator {
    HashMap<String, ApplicationCode> applicationCodeCache = new HashMap<>();
    HashMap<String, StandardApplicant> standardApplicantCache = new HashMap<>();

    public BulkUploadApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            ApplicationFeeService feeService,
            BusinessDateProvider businessDateProvider,
            StandardApplicantRepository standardApplicantRepository) {
        super(
                applicationListRepository,
                applicationCodeRepository,
                feeService,
                businessDateProvider,
                standardApplicantRepository);
    }

    @Override
    public <R> R validate(
            PayloadForCreate<EntryCreateDto> validatable,
            BiFunction<PayloadForCreate<EntryCreateDto>, CreateApplicationEntryValidationSuccess, R>
                    validateSuccess) {

        if (applicationCodeCache.isEmpty()) {
            cacheApplicationCodes();
        }

        if (standardApplicantCache.isEmpty()) {
            cacheStandardApplicants();
        }

        ensureRespondentMutualExclusion(validatable);

        ensureApplicantMutualExclusion(validatable);

        validateOfficialCounts(validatable);

        val code = validateApplicationCode(validatable);
        StandardApplicant sa = validateStandardApplicant(validatable);

        // parse the wording template and error if not valid
        WordingTemplateSentence wordingTemplateCollection =
                WordingTemplateSentence.with(code.getWording());

        validateLodgementDate(validatable);

        // if fee is due get the fee
        FeePair fee = validateFee(code, validatable);

        // validate the respondent if required
        validateRespondent(code, validatable);

        if (validateSuccess != null) {
            return validateSuccess.apply(
                    validatable,
                    getResult(code, wordingTemplateCollection, fee, sa, null, validatable));
        }
        return null;
    }

    @Override
    protected boolean isFeeStatusRequired(ApplicationCode applicationCode) {
        return false;
    }

    private void cacheApplicationCodes() {

        for (ApplicationCode code :
                applicationCodeRepository.findAllCodesByDate(
                        super.businessDateProvider.currentUkDate())) {
            applicationCodeCache.put(code.getCode(), code);
        }
    }

    private void cacheStandardApplicants() {
        for (StandardApplicant sa :
                standardApplicantRepository.findAllStandardApplicantByDate(
                        super.businessDateProvider.currentUkDate())) {
            standardApplicantCache.put(sa.getApplicantCode(), sa);
        }
    }

    private ApplicationCode validateApplicationCode(PayloadForCreate<EntryCreateDto> validatable) {
        if (getApplicationCode(validatable) != null
                && ApplicationCodeTypeEnum.isMatching(
                        ApplicationCodeTypeEnum.ENFORCEMENT_FINES, getApplicationCode(validatable))
                && (getAccountNumber(validatable) == null
                        || getAccountNumber(validatable).isEmpty())) {
            throw new AppRegistryException(
                    AppListEntryError.ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE,
                    "Application number required for application code %s"
                            .formatted(getApplicationCode(validatable)));
        }

        // validate that the application code exists and is valid for today
        ApplicationCode code = applicationCodeCache.get(getApplicationCode(validatable));

        if (code == null) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST,
                    "No valid code can be found %s".formatted(getApplicationCode(validatable)));
        }

        log.debug("Validated the application code {}", getApplicationCode(validatable));
        return code;
    }

    private StandardApplicant validateStandardApplicant(
            PayloadForCreate<EntryCreateDto> validatable) {
        String standardApplicantCode = getStandardApplicantCode(validatable);
        val saCode = standardApplicantCache.get(getStandardApplicantCode(validatable));

        if (saCode == null) {
            // throw exception we expect a valid standard applicant code
            throw new AppRegistryException(
                    AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST,
                    "The standard applicant does not exist %s".formatted(standardApplicantCode));
        }

        log.debug("Validated standard applicant {}", standardApplicantCode);

        return saCode;
    }

    public void validateApplicationList(UUID applicationListUuid) {
        validateParentApplicationList(applicationListUuid);
    }
}
