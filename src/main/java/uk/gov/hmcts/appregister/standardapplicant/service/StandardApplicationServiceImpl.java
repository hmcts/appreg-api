package uk.gov.hmcts.appregister.standardapplicant.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.IndividualOrOrganisation;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapper;

/**
 * Service implementation for managing standard applicants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StandardApplicationServiceImpl implements StandardApplicantService {

    private final StandardApplicantRepository repository;
    private final StandardApplicantMapper mapper;
    private final Clock clock;
    private final ZoneId ukZone;
    private final PageMapper pageMapper;

    @Override
    public StandardApplicantPage findAll(String code, String name, Pageable pageable) {
        log.debug(
                "Start: Find Standard Applicant for: app code: {} name: {} with paging: {}",
                code,
                name,
                pageable);

        // Use today's date to ensure we only return Result Codes that are currently active.
        var todayUk = LocalDate.now(clock.withZone(ukZone));

        // breaks name into individual and/or organisation parts
        IndividualOrOrganisation individualOrOrganisationSearch = IndividualOrOrganisation.of(name);
        String nm = individualOrOrganisationSearch.getName();
        String surname = individualOrOrganisationSearch.getSurname();

        final Page<StandardApplicant> standardApplicantsList =
                repository.search(code, nm, surname, todayUk, pageable);

        StandardApplicantPage newPage = new StandardApplicantPage();
        pageMapper.toPage(standardApplicantsList, newPage);

        // Map each entity to a summary DTO and add to the page content
        standardApplicantsList.map(
                sa -> {
                    return newPage.addContentItem(mapper.toReadGetSummaryDto(sa));
                });

        log.debug(
                "Finished: Find Standard Applicant for: code: {} name: {} with paging: {}",
                code,
                name,
                pageable);
        return newPage;
    }

    @Override
    @Deprecated
    public StandardApplicantDto findById(Long id) {
        final StandardApplicant standardApplicant =
                repository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Standard applicant not found"));

        return mapper.toReadDto(standardApplicant);
    }
}
