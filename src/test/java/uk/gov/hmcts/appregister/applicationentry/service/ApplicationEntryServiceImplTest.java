package uk.gov.hmcts.appregister.applicationentry.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapperImpl;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplicationEntryServiceImplTest {
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @Spy private final ApplicationListEntryMapper mapper = new ApplicationListEntryMapperImpl();

    @Spy private final PageMapper pageMapper = new PageMapper();

    @InjectMocks private ApplicationEntryServiceImpl applicationEntryService;

    @BeforeEach
    public void setUp() {
        mapper.setApplicantMapper(new ApplicantMapperImpl());
    }

    @Test
    public void testSearchForGetSummary() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryGetFilterDto entryGetFilterDto =
                Instancio.of(EntryGetFilterDto.class).withSettings(settings).create();
        ApplicationListEntryGetSummaryProjection applicationListEntryGetSummaryProjection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        when(applicationListEntryGetSummaryProjection.getApplicationOrganisation())
                .thenReturn("org1");
        when(applicationListEntryGetSummaryProjection.getApplicantSurname()).thenReturn("surname");
        when(applicationListEntryGetSummaryProjection.getAnameAddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getRnameAddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getDateOfAl()).thenReturn(LocalDate.now());

        when(applicationListEntryGetSummaryProjection.getAccountReference()).thenReturn("accref");
        when(applicationListEntryGetSummaryProjection.getCjaCode()).thenReturn("cjacode");
        when(applicationListEntryGetSummaryProjection.getCourtCode()).thenReturn("courtcode");
        when(applicationListEntryGetSummaryProjection.getLegislation()).thenReturn("leg");
        when(applicationListEntryGetSummaryProjection.getTitle()).thenReturn("title");

        when(applicationListEntryGetSummaryProjection.getRespondentSurname())
                .thenReturn("ressurname");
        when(applicationListEntryGetSummaryProjection.getResult()).thenReturn(null);
        when(applicationListEntryGetSummaryProjection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(applicationListEntryGetSummaryProjection.getStatus()).thenReturn(Status.OPEN);

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(1);

        Page<ApplicationListEntryGetSummaryProjection> page =
                new PageImpl<ApplicationListEntryGetSummaryProjection>(
                        List.of(applicationListEntryGetSummaryProjection), mockPage, 1);

        when(applicationListEntryRepository.searchForGetSummary(
                        eq(true),
                        eq(entryGetFilterDto.getDate()),
                        eq(entryGetFilterDto.getCourtCode()),
                        eq(entryGetFilterDto.getOtherLocationDescription()),
                        eq(entryGetFilterDto.getCjaCode()),
                        eq(entryGetFilterDto.getApplicantOrganisation()),
                        eq(entryGetFilterDto.getApplicantSurname()),
                        eq(entryGetFilterDto.getStandardApplicantCode()),
                        eq(Status.fromValue(entryGetFilterDto.getStatus().getValue())),
                        eq(entryGetFilterDto.getRespondentOrganisation()),
                        eq(entryGetFilterDto.getRespondentSurname()),
                        eq(entryGetFilterDto.getRespondentPostcode()),
                        eq(entryGetFilterDto.getAccountReference()),
                        eq(mockPage)))
                .thenReturn(page);

        // execute
        EntryPage entryPage = applicationEntryService.search(entryGetFilterDto, mockPage);

        // assert
        Assertions.assertEquals(1, entryPage.getContent().size());
        Assertions.assertEquals(
                ApplicationListStatus.OPEN, entryPage.getContent().get(0).getStatus());
        Assertions.assertEquals("leg", entryPage.getContent().get(0).getLegislation());
        Assertions.assertEquals("title", entryPage.getContent().get(0).getApplicationTitle());

        Assertions.assertNotNull(entryPage.getContent().get(0).getApplicant());
        Assertions.assertNotNull(entryPage.getContent().get(0).getRespondent());
    }
}
