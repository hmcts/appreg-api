package uk.gov.hmcts.appregister.standardapplicant.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapperImpl;

@ExtendWith(MockitoExtension.class)
public class StandardApplicantServiceTest {

    @Mock private StandardApplicantRepository repository;

    @Spy
    private StandardApplicantMapperImpl standardApplicantMapper = new StandardApplicantMapperImpl();

    @Mock private Clock clock;

    @Spy private ZoneId ukZone = ZoneId.of("Europe/London");

    @Spy private PageMapper pageMapper = new PageMapper();

    @InjectMocks private StandardApplicationServiceImpl standardApplicantService;

    @BeforeEach
    public void before() {
        when(clock.instant()).thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.eq(ukZone))).thenReturn(clock);

        standardApplicantMapper.setApplicantMapper(new ApplicantMapperImpl());
    }

    @Test
    public void testGetAll() {
        String code = "APP001";
        String name = "John Doe";
        Pageable pageable = PageRequest.of(0, 2);

        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();

        PageImpl<StandardApplicant> pageImpl =
                new PageImpl<>(
                        java.util.List.of(
                                standardApplicantTestData.someComplete(),
                                standardApplicantTestData.someComplete()),
                        pageable,
                        2);

        when(repository.search(eq(code), eq(name), isNotNull(), eq(pageable))).thenReturn(pageImpl);

        PagingWrapper wrapper = PagingWrapper.of(List.of(), pageable);

        StandardApplicantPage standardApplicantPage =
                standardApplicantService.findAll(code, name, wrapper);

        Assertions.assertEquals(2, standardApplicantPage.getTotalElements());
        Assertions.assertEquals(
                pageImpl.getContent().get(0).getApplicantCode(),
                standardApplicantPage.getContent().get(0).getCode());
        Assertions.assertEquals(
                pageImpl.getContent().get(0).getName(),
                standardApplicantPage
                        .getContent()
                        .get(0)
                        .getApplicant()
                        .getOrganisation()
                        .getName());
        Assertions.assertEquals(
                pageImpl.getContent().get(0).getApplicantStartDate(),
                standardApplicantPage.getContent().get(0).getStartDate());
        Assertions.assertEquals(
                pageImpl.getContent().get(0).getApplicantEndDate(),
                standardApplicantPage.getContent().get(0).getEndDate().get());

        Assertions.assertEquals(
                pageImpl.getContent().get(1).getApplicantCode(),
                standardApplicantPage.getContent().get(1).getCode());
        Assertions.assertEquals(
                pageImpl.getContent().get(1).getName(),
                standardApplicantPage
                        .getContent()
                        .get(1)
                        .getApplicant()
                        .getOrganisation()
                        .getName());
        Assertions.assertEquals(
                pageImpl.getContent().get(1).getApplicantStartDate(),
                standardApplicantPage.getContent().get(1).getStartDate());
        Assertions.assertEquals(
                pageImpl.getContent().get(1).getApplicantEndDate(),
                standardApplicantPage.getContent().get(1).getEndDate().get());
    }
}
