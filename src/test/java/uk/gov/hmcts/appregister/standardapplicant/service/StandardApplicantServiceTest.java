package uk.gov.hmcts.appregister.standardapplicant.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
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
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
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
        String addressLine1 = "123 Main Street";
        LocalDate from = LocalDate.now().minusDays(10);
        LocalDate to = LocalDate.now().plusDays(10);
        Pageable pageable = PageRequest.of(0, 2);

        StandardApplicant standardApplicant1 = mock(StandardApplicant.class);
        StandardApplicant standardApplicant2 = mock(StandardApplicant.class);

        when(standardApplicant1.getApplicantCode()).thenReturn("APP001");
        when(standardApplicant1.getName()).thenReturn("John Doe");
        when(standardApplicant1.getApplicantStartDate()).thenReturn(from);
        when(standardApplicant1.getApplicantEndDate()).thenReturn(to);

        when(standardApplicant2.getApplicantCode()).thenReturn("APP002");
        when(standardApplicant2.getName()).thenReturn("Jane Doe");
        when(standardApplicant2.getApplicantStartDate()).thenReturn(from.plusDays(1));
        when(standardApplicant2.getApplicantEndDate()).thenReturn(to.plusDays(1));

        StandardApplicantEnrichedProjection projection1 =
                mock(StandardApplicantEnrichedProjection.class);
        StandardApplicantEnrichedProjection projection2 =
                mock(StandardApplicantEnrichedProjection.class);

        when(projection1.getStandardApplicant()).thenReturn(standardApplicant1);
        when(projection1.getEffectiveName()).thenReturn("John Doe");

        when(projection2.getStandardApplicant()).thenReturn(standardApplicant2);
        when(projection2.getEffectiveName()).thenReturn("Jane Doe");

        PageImpl<StandardApplicantEnrichedProjection> pageImpl =
                new PageImpl<>(java.util.List.of(projection1, projection2), pageable, 2);

        when(repository.search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(pageable)))
                .thenReturn(pageImpl);

        PagingWrapper wrapper = PagingWrapper.of(List.of(), pageable);

        StandardApplicantPage standardApplicantPage =
                standardApplicantService.findAll(code, name, addressLine1, from, to, wrapper);

        verify(repository)
                .search(
                        eq(code),
                        eq(name),
                        eq(addressLine1),
                        eq(from),
                        eq(to),
                        isNotNull(),
                        eq(pageable));

        Assertions.assertEquals(2, standardApplicantPage.getTotalElements());
        Assertions.assertEquals(
                pageImpl.getContent().getFirst().getStandardApplicant().getApplicantCode(),
                standardApplicantPage.getContent().getFirst().getCode());
        Assertions.assertEquals(
                pageImpl.getContent().getFirst().getStandardApplicant().getName(),
                standardApplicantPage
                        .getContent()
                        .getFirst()
                        .getApplicant()
                        .getOrganisation()
                        .getName());
        Assertions.assertEquals(
                pageImpl.getContent().get(0).getStandardApplicant().getApplicantStartDate(),
                standardApplicantPage.getContent().get(0).getStartDate());
        Assertions.assertEquals(
                pageImpl.getContent().get(0).getStandardApplicant().getApplicantEndDate(),
                standardApplicantPage.getContent().get(0).getEndDate().get());

        Assertions.assertEquals(
                pageImpl.getContent().get(1).getStandardApplicant().getApplicantCode(),
                standardApplicantPage.getContent().get(1).getCode());
        Assertions.assertEquals(
                pageImpl.getContent().get(1).getStandardApplicant().getName(),
                standardApplicantPage
                        .getContent()
                        .get(1)
                        .getApplicant()
                        .getOrganisation()
                        .getName());
        Assertions.assertEquals(
                pageImpl.getContent().get(1).getStandardApplicant().getApplicantStartDate(),
                standardApplicantPage.getContent().get(1).getStartDate());
        Assertions.assertEquals(
                pageImpl.getContent().get(1).getStandardApplicant().getApplicantEndDate(),
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

    @Test
    void testGetByCode_auditsRequestedLookupCriteria() {
        String code = "APP001";
        StandardApplicant standardApplicant = new StandardApplicant();
        standardApplicant.setApplicantCode(code);
        standardApplicant.setName("John Doe");
        standardApplicant.setApplicantStartDate(LocalDate.of(2020, 1, 1));
        validator.setSuccess(standardApplicant);

        CapturingAuditListener listener = new CapturingAuditListener();
        StandardApplicationServiceImpl localService =
                new StandardApplicationServiceImpl(
                        repository,
                        standardApplicantMapper,
                        clock,
                        ukZone,
                        pageMapper,
                        validator,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener),
                        new ApplicantMapperImpl());

        LocalDate date = LocalDate.of(2025, 1, 1);
        StandardApplicantGetDetailDto actual = localService.findByCode(code, date);

        Assertions.assertEquals(code, actual.getCode());
        Assertions.assertNotNull(listener.getCompleteEvent());
        StandardApplicant audited = (StandardApplicant) listener.getCompleteEvent().getNewValue();
        Assertions.assertNotSame(standardApplicant, audited);
        Assertions.assertEquals(code, audited.getApplicantCode());
        Assertions.assertEquals(date, audited.getApplicantStartDate());
    }

    private static final class CapturingAuditListener implements AuditOperationLifecycleListener {
        private CompleteEvent completeEvent;

        @Override
        public void eventPerformed(BaseAuditEvent event) {
            if (event instanceof CompleteEvent complete) {
                completeEvent = complete;
            }
        }

        private CompleteEvent getCompleteEvent() {
            return completeEvent;
        }
    }

    @Setter
    static class DummyStandardApplicantExistsValidator extends StandardApplicantExistsValidator {
        private StandardApplicant success;

        public DummyStandardApplicantExistsValidator(StandardApplicantRepository repository) {
            super(repository);
        }

        @Override
        public <R> R validate(
                PayloadForGet saId,
                BiFunction<PayloadForGet, StandardApplicant, R> createApplicationSupplier) {
            return createApplicationSupplier.apply(
                    saId, success != null ? success : defaultApplicant());
        }

        private StandardApplicant defaultApplicant() {
            StandardApplicant standardApplicant = new StandardApplicant();
            standardApplicant.setApplicantCode("APP001");
            standardApplicant.setName("John Doe");
            standardApplicant.setApplicantStartDate(LocalDate.now());
            standardApplicant.setApplicantEndDate(LocalDate.now().plusDays(1));
            return standardApplicant;
        }
    }
}
