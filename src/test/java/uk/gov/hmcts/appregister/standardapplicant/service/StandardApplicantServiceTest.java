package uk.gov.hmcts.appregister.standardapplicant.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiFunction;
import lombok.Setter;
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
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.audit.StandardApplicantOperation;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapperImpl;
import uk.gov.hmcts.appregister.standardapplicant.validator.StandardApplicantExistsValidator;

@ExtendWith(MockitoExtension.class)
public class StandardApplicantServiceTest {

    @Mock private StandardApplicantRepository repository;

    @Spy
    private DummyStandardApplicantExistsValidator validator =
            new DummyStandardApplicantExistsValidator(repository);

    @Spy
    private List<AuditOperationLifecycleListener> listeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService =
            new AuditOperationServiceImpl(new ObjectMapper(), listeners);

    @Spy
    private StandardApplicantMapperImpl standardApplicantMapper = new StandardApplicantMapperImpl();

    @Mock private Clock clock;

    @Spy private ZoneId ukZone = ZoneId.of("Europe/London");

    @Spy private PageMapper pageMapper = new PageMapper();

    @InjectMocks private StandardApplicationServiceImpl standardApplicantService;

    @BeforeEach
    public void before() {
        standardApplicantMapper.setApplicantMapper(new ApplicantMapperImpl());
    }

    @Test
    public void testGetAll() {
        when(clock.instant()).thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.eq(ukZone))).thenReturn(clock);

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

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(StandardApplicantOperation.GET_STANDARD_APPLICANTS),
                        notNull(),
                        notNull());
    }

    @Test
    public void testGetByCode() {
        String code = "APP001";
        LocalDate date = LocalDate.now();

        StandardApplicantGetDetailDto standardApplicantGetDetailDto =
                standardApplicantService.findByCode(code, date);

        Assertions.assertEquals(standardApplicantGetDetailDto.getCode(), code);
        Assertions.assertEquals(standardApplicantGetDetailDto.getStartDate(), date);

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(StandardApplicantOperation.GET_STANDARD_APPLICANTS_BY_CODE_AND_DATE),
                        notNull(),
                        notNull());
    }

    @Setter
    static class DummyStandardApplicantExistsValidator extends StandardApplicantExistsValidator {
        public DummyStandardApplicantExistsValidator(StandardApplicantRepository repository) {
            super(repository);
        }

        @Override
        public <R> R validate(
                PayloadForGet saId,
                BiFunction<PayloadForGet, StandardApplicant, R> createApplicationSupplier) {
            return createApplicationSupplier.apply(saId, validateId());
        }

        private StandardApplicant validateId() {
            StandardApplicant standardApplicant = new StandardApplicant();
            standardApplicant.setApplicantCode("APP001");
            standardApplicant.setName("John Doe");
            standardApplicant.setApplicantStartDate(LocalDate.now());
            standardApplicant.setApplicantEndDate(LocalDate.now().plusDays(1));
            return standardApplicant;
        }
    }
}
