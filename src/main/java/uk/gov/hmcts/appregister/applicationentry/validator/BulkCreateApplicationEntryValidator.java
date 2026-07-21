package uk.gov.hmcts.appregister.applicationentry.validator;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

/**
 * Validates application entries created from bulk upload rows.
 */
@Component
public class BulkCreateApplicationEntryValidator extends CreateApplicationEntryValidator {

    private final ApplicationCodeRepository applicationCodeRepository;
    private final ApplicationFeeService feeService;
    private final BusinessDateProvider businessDateProvider;
    private final StandardApplicantRepository standardApplicantRepository;

    public BulkCreateApplicationEntryValidator(
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
        this.applicationCodeRepository = applicationCodeRepository;
        this.feeService = feeService;
        this.businessDateProvider = businessDateProvider;
        this.standardApplicantRepository = standardApplicantRepository;
    }

    @Override
    protected boolean isFeeStatusRequired(ApplicationCode applicationCode) {
        return false;
    }

    public ApplicationList validateApplicationList(UUID applicationListUuid) {
        return validateParentApplicationList(applicationListUuid);
    }

    /** Creates one reference-data cache for a single import job. */
    public Session createSession(ApplicationList applicationList) {
        var businessDate = businessDateProvider.currentUkDate();
        var applicationCodes =
                applicationCodeRepository.findAllByDate(businessDate).stream()
                        .collect(
                                Collectors.groupingBy(
                                        code -> normalise(code.getCode()),
                                        LinkedHashMap::new,
                                        Collectors.toList()))
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        entry ->
                                                ReferenceDataSelectionUtil
                                                        .selectFirstOrderedActiveRecord(
                                                                entry.getValue(),
                                                                "application code",
                                                                entry.getKey(),
                                                                businessDate,
                                                                ApplicationCode::getEndDate)));
        var standardApplicants =
                standardApplicantRepository.findAllByDate(businessDate).stream()
                        .collect(
                                Collectors.groupingBy(
                                        applicant -> normalise(applicant.getApplicantCode()),
                                        LinkedHashMap::new,
                                        Collectors.toList()))
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        entry ->
                                                ReferenceDataSelectionUtil
                                                        .selectFirstOrderedActiveRecord(
                                                                entry.getValue(),
                                                                "standard applicant",
                                                                entry.getKey(),
                                                                businessDate,
                                                                StandardApplicant
                                                                        ::getApplicantEndDate)));

        return new Session(applicationList, applicationCodes, standardApplicants);
    }

    @Override
    protected CreateApplicationEntryValidationSuccess getResult(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            FeePair fee,
            StandardApplicant saCode,
            ApplicationList applicationList,
            PayloadForCreate<EntryCreateDto> dto) {
        dto.getData()
                .setWordingFields(
                        trimAndKeyWordingFields(
                                code, wordingTemplateCollection, dto.getData().getWordingFields()));

        return super.getResult(code, wordingTemplateCollection, fee, saCode, applicationList, dto);
    }

    private static List<TemplateSubstitution> trimAndKeyWordingFields(
            ApplicationCode code,
            WordingTemplateSentence wordingTemplateCollection,
            List<TemplateSubstitution> suppliedWordingFields) {
        List<TemplateSubstitution> requiredWordingFields =
                wordingTemplateCollection.getKeysToBeSubstituted();
        List<TemplateSubstitution> supplied =
                suppliedWordingFields == null ? List.of() : suppliedWordingFields;

        if (requiredWordingFields.isEmpty()) {
            return List.of();
        }

        if (supplied.size() < requiredWordingFields.size()) {
            throw new AppRegistryException(
                    CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                    "Not enough APPLICATION_TEXT values supplied for code %s"
                            .formatted(code.getCode()),
                    Map.of(
                            "templateSize",
                            Integer.toString(requiredWordingFields.size()),
                            "valueSize",
                            Integer.toString(supplied.size())));
        }

        return IntStream.range(0, requiredWordingFields.size())
                .mapToObj(
                        index -> {
                            TemplateSubstitution suppliedField = supplied.get(index);
                            String suppliedValue =
                                    suppliedField == null ? null : suppliedField.getValue();
                            if (StringUtils.isBlank(suppliedValue)) {
                                throw new AppRegistryException(
                                        CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH,
                                        "APPLICATION_TEXT%s is required for code %s"
                                                .formatted(index + 1, code.getCode()),
                                        Map.of(
                                                "templateSize",
                                                Integer.toString(requiredWordingFields.size()),
                                                "valueSize",
                                                Integer.toString(supplied.size())));
                            }

                            return new TemplateSubstitution(
                                    requiredWordingFields.get(index).getKey(), suppliedValue);
                        })
                .toList();
    }

    private static String normalise(String value) {
        return StringUtils.lowerCase(value, Locale.ROOT).trim();
    }

    /** Job-scoped validation state. A session is confined to one async lifecycle. */
    public final class Session {
        private final ApplicationList applicationList;
        private final Map<String, ApplicationCode> applicationCodes;
        private final Map<String, StandardApplicant> standardApplicants;
        private final Map<FeeLookup, FeePair> fees = new HashMap<>();

        private Session(
                ApplicationList applicationList,
                Map<String, ApplicationCode> applicationCodes,
                Map<String, StandardApplicant> standardApplicants) {
            this.applicationList = applicationList;
            this.applicationCodes = applicationCodes;
            this.standardApplicants = standardApplicants;
        }

        /** Validates one row without repeating application-list or reference-data queries. */
        public <R> R validate(
                PayloadForCreate<EntryCreateDto> validatable,
                BiFunction<
                                PayloadForCreate<EntryCreateDto>,
                                CreateApplicationEntryValidationSuccess,
                                R>
                        validateSuccess) {
            return validateUsing(
                    validatable,
                    validateSuccess,
                    ignored -> applicationList,
                    this::standardApplicant,
                    this::applicationCode,
                    this::fee);
        }

        private StandardApplicant standardApplicant(PayloadForCreate<EntryCreateDto> validatable) {
            var applicantCode = validatable.getData().getStandardApplicantCode();
            if (applicantCode == null) {
                return null;
            }

            var applicant = standardApplicants.get(normalise(applicantCode));
            if (applicant == null) {
                throw new AppRegistryException(
                        AppListEntryError.STANDARD_APPLICANT_DOES_NOT_EXIST,
                        "The standard applicant does not exist %s".formatted(applicantCode));
            }
            return applicant;
        }

        private ApplicationCode applicationCode(PayloadForCreate<EntryCreateDto> validatable) {
            var requestedCode = validatable.getData().getApplicationCode();
            var applicationCode = applicationCodes.get(normalise(requestedCode));
            if (applicationCode == null) {
                throw new AppRegistryException(
                        AppListEntryError.APPLICATION_CODE_DOES_NOT_EXIST,
                        "No valid code can be found %s".formatted(requestedCode));
            }
            return applicationCode;
        }

        private FeePair fee(
                ApplicationCode applicationCode, PayloadForCreate<EntryCreateDto> validatable) {
            var lookup =
                    new FeeLookup(applicationCode.getFeeReference(), getLodgementDate(validatable));
            return fees.computeIfAbsent(
                    lookup, key -> feeService.resolveFeePair(key.reference(), key.lodgementDate()));
        }
    }

    private record FeeLookup(String reference, LocalDate lodgementDate) {}
}
