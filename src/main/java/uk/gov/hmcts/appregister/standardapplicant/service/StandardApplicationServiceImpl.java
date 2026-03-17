package uk.gov.hmcts.appregister.standardapplicant.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.audit.StandardApplicantOperation;
import uk.gov.hmcts.appregister.standardapplicant.mapper.CodeAndName;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapper;
import uk.gov.hmcts.appregister.standardapplicant.validator.StandardApplicantExistsValidator;

/**
 * Service implementation for managing standard applicants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StandardApplicationServiceImpl implements StandardApplicantService {
    private final StandardApplicantRepository repository;
    private final StandardApplicantMapper mapper;
    private final Clock clock;
    private final ZoneId ukZone;
    private final PageMapper pageMapper;

    private final StandardApplicantExistsValidator validator;

    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final ApplicantMapper applicantMapper;

    @Override
    @Transactional(readOnly = true)
    public StandardApplicantPage findAll(String code, String name, PagingWrapper pageable) {
        return auditService.processAudit(
                null,
                StandardApplicantOperation.GET_STANDARD_APPLICANTS,
                (req) -> {
                    // Use today's date to ensure we only return Result Codes that are currently
                    // active.
                    var todayUk = LocalDate.now(clock.withZone(ukZone));

                    // breaks name into individual and/or organisation parts
                    final Page<StandardApplicant> standardApplicantsList =
                            repository.search(code, name, todayUk, pageable.getPageable());

                    StandardApplicantPage newPage = new StandardApplicantPage();
                    pageMapper.toPage(standardApplicantsList, newPage, pageable.getSortStrings());

                    // Map each entity to a summary DTO and add to the page content
                    standardApplicantsList.map(
                            sa -> newPage.addContentItem(mapper.toReadGetSummaryDto(sa)));

                    CodeAndName record = new CodeAndName(code, name);
                    AuditableResult<StandardApplicantPage, StandardApplicant> result =
                            new AuditableResult<>(newPage, mapper.toEntity(record));

                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    @Transactional(readOnly = true)
    public StandardApplicantGetDetailDto findByCode(String code, LocalDate date) {
        return auditService.processAudit(
                null,
                StandardApplicantOperation.GET_STANDARD_APPLICANTS_BY_CODE_AND_DATE,
                (req) -> {
                    log.debug(
                            "Start: Find Standard Applicant By Code for: app code: {} date: {}",
                            code,
                            date);

                    StandardApplicantGetDetailDto payloadForGet =
                            validator.validate(
                                    PayloadForGet.builder().date(date).code(code).build(),
                                    (id, standardApplicant) ->
                                            mapper.toReadGetDto(standardApplicant));

                    AuditableResult<StandardApplicantGetDetailDto, StandardApplicant> result =
                            new AuditableResult<>(payloadForGet, mapper.toEntity(code, date));

                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
