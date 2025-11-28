package uk.gov.hmcts.appregister.applicationentry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryServiceImpl implements ApplicationEntryService {

    private final ApplicationListEntryMapper mapper;

    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final PageMapper pageMapper;

    @Override
    public EntryPage search(EntryGetFilterDto filterDto, Pageable pageable) {
        Status status = mapper.toStatus(filterDto.getStatus());

        log.debug(
                "Started: Find Application Entry for criteria: {} with paging: {}",
                filterDto,
                pageable);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                applicationListEntryRepository.searchForGetSummary(
                        filterDto.getDate() != null,
                        filterDto.getDate(),
                        filterDto.getCourtCode(),
                        filterDto.getOtherLocationDescription(),
                        filterDto.getCjaCode(),
                        filterDto.getApplicantOrganisation(),
                        filterDto.getApplicantSurname(),
                        filterDto.getStandardApplicantCode(),
                        status,
                        filterDto.getRespondentOrganisation(),
                        filterDto.getRespondentSurname(),
                        filterDto.getRespondentPostcode(),
                        filterDto.getAccountReference(),
                        pageable);

        // breaks name into individual and/or organisation parts
        EntryPage newPage = new EntryPage();
        pageMapper.toPage(resultPage, newPage);

        // Map each entity to a summary DTO and add to the page content
        resultPage.map(
                entry -> {
                    return newPage.addContentItem(mapper.toEntrySummary(entry));
                });

        log.debug(
                "Finished: Find Application Entry for criteria: {} with paging: {}",
                filterDto,
                pageable);
        return newPage;
    }
}
