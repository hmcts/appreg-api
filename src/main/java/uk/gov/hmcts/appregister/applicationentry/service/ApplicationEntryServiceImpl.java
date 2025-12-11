package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.persistence.OneToMany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.validator.GetEntryValidator;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryServiceImpl implements ApplicationEntryService {

    private final ApplicationListEntryMapper mapper;

    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final GetEntryValidator getEntryValidator;


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
        resultPage.forEach(
                entry -> {
                    newPage.addContentItem(mapper.toEntrySummary(entry));
                });

        log.debug(
                "Finished: Find Application Entry for criteria: {} with paging: {}",
                filterDto,
                pageable);
        return newPage;
    }

    @Override
    public MatchResponse<EntryGetDetailDto> getApplicationListEntrySummary(PayloadGetEntryInList entry) {
        return getEntryValidator.validate(entry, (req, success) -> {
          getKeyablesForCreateUpdateEtag(success.getApplicationListEntry());
            EntryGetDetailDto dto = mapper.toEntryGetDetailDto(success.getApplicationListEntry());
          return MatchResponse.of(dto, getKeyablesForCreateUpdateEtag(success.getApplicationListEntry()));
      });
    }

    /**
     * gets the keyable for the create/update entry.
     *
     * @param entryForEtag The entry that was created or is being updated
     * @return The list of keyables that constitute an etag
     */
    private List<Keyable> getKeyablesForCreateUpdateEtag(ApplicationListEntry entryForEtag) {
        List<AppListEntryOfficial> officialList = entryForEtag.getOfficials();
        List<AppListEntryFeeStatus> statusList = entryForEtag.getEntryFeeStatuses();
        List<AppListEntryFeeId> feesEntryList = entryForEtag.getEntryFeeIds();
        List<Fee> feesList = new ArrayList<>();

        // get all the fees information that can be updated
        for (AppListEntryFeeId fee : feesEntryList) {
            feesList.add(fee.getFeeId());
        }

        // create the update etag based on the following details
        List<Keyable> keyables = new ArrayList<>();
        keyables.add(updateEntry);
        keyables.addAll(officialList);
        keyables.addAll(statusList);
        keyables.addAll(feesList);
        return keyables;
    }

    @OneToMany(mappedBy = "appListEntry")
    private List<AppListEntryOfficial> officials;

    @OneToMany(mappedBy = "appListEntry")
    private List<AppListEntryFeeStatus> entryFeeStatuses;

    @OneToMany(mappedBy = "appListEntryId")
    private List<AppListEntryFeeId> entryFeeIds;
}
