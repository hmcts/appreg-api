package uk.gov.hmcts.appregister.courtlocation.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.courtlocation.audit.CourtLocationAuditOperation;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.courtlocation.mapper.CourtLocationMapper;
import uk.gov.hmcts.appregister.courtlocation.mapper.CourtLocationMapperImpl;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;

@ExtendWith(MockitoExtension.class)
public class CourtLocationServiceImplTest {

    @Mock private NationalCourtHouseRepository repository;

    @Spy
    private List<AuditOperationLifecycleListener> auditListeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService =
            new AuditOperationServiceImpl(new ObjectMapper(), auditListeners);

    @Spy private CourtLocationMapper mapper = new CourtLocationMapperImpl();

    @Mock private PageMapper pageMapper;

    @InjectMocks private CourtLocationServiceImpl service;

    /**
     * Given exactly one active courthouse row for (code,date), the service maps it to a detail DTO
     * and returns 200-style data.
     */
    @Test
    void findByCodeAndDate_success_singleRow() {
        final String code = "ABC123";
        final LocalDate date = LocalDate.parse("2025-01-01");

        var entity = new NationalCourtHouse();
        entity.setCourtLocationCode(code);
        entity.setName("Bath Crown Court");
        entity.setStartDate(LocalDate.parse("2020-01-01"));
        // endDate is being left as null => still active

        when(repository.findActiveCourtsWithDate(code, date)).thenReturn(List.of(entity));

        CourtLocationGetDetailDto dto = service.findByCodeAndDate(code, date);

        Assertions.assertEquals("Bath Crown Court", dto.getName());
        Assertions.assertEquals(code, dto.getLocationCode());
        Assertions.assertEquals(LocalDate.parse("2020-01-01"), dto.getStartDate());

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * When the repository returns no rows for (code,date), the service throws AppRegistryException
     * with COURT_NOT_FOUND.
     */
    @Test
    void findByCodeAndDate_notFound_throws() {
        String code = "MISSING";
        LocalDate date = LocalDate.parse("2025-01-01");

        when(repository.findActiveCourtsWithDate(code, date)).thenReturn(List.of());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.findByCodeAndDate(code, date));
        Assertions.assertEquals(CourtLocationError.COURT_NOT_FOUND, ex.getCode());

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * When multiple rows exist for (code,date), the service throws AppRegistryException with
     * DUPLICATE_COURT_FOUND.
     */
    @Test
    void findByCodeAndDate_duplicate_throws() {
        String code = "DUP";
        LocalDate date = LocalDate.parse("2025-01-01");

        var courtHouse1 = new NationalCourtHouse();
        var courtHouse2 = new NationalCourtHouse();
        when(repository.findActiveCourtsWithDate(code, date))
                .thenReturn(List.of(courtHouse1, courtHouse2));

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.findByCodeAndDate(code, date));
        Assertions.assertEquals(CourtLocationError.DUPLICATE_COURT_FOUND, ex.getCode());

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CourtLocationAuditOperation.GET_COURT_LOCATION_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * For a non-empty page: repository page is mapped to API page, page metadata is copied, and
     * each entity becomes a summary DTO.
     */
    @Test
    void getPage_success_mapsContentAndMeta() {
        final String codeFilter = "A";
        final String nameFilter = "X";
        final var pageable = PageRequest.of(0, 2);

        var e1 = new NationalCourtHouse();
        e1.setName("Alpha");
        e1.setCourtLocationCode("A1");
        var e2 = new NationalCourtHouse();
        e2.setName("Beta");
        e2.setCourtLocationCode("B1");
        Page<NationalCourtHouse> dbPage = new PageImpl<>(List.of(e1, e2), pageable, 5);

        when(repository.findAllActiveCourts(codeFilter, nameFilter, pageable)).thenReturn(dbPage);

        PagingWrapper wrapper = PagingWrapper.of(List.of(), pageable);

        // Simulate page meta copy so assertions have values.
        doAnswer(
                        inv -> {
                            CourtLocationPage out = inv.getArgument(1);
                            out.setTotalElements(dbPage.getTotalElements());
                            out.setTotalPages(dbPage.getTotalPages());
                            out.setPageNumber(dbPage.getNumber());
                            out.setPageSize(dbPage.getSize());
                            return null;
                        })
                .when(pageMapper)
                .toPage(
                        eq(dbPage),
                        ArgumentMatchers.any(CourtLocationPage.class),
                        eq(wrapper.getSortStrings()));

        CourtLocationPage pageDto = service.getPage(nameFilter, codeFilter, wrapper);

        Assertions.assertEquals(5, pageDto.getTotalElements());
        Assertions.assertEquals(3, pageDto.getTotalPages());
        Assertions.assertEquals(0, pageDto.getPageNumber());
        Assertions.assertEquals(2, pageDto.getPageSize());
        Assertions.assertEquals(2, pageDto.getContent().size());

        var s1 = pageDto.getContent().get(0);
        var s2 = pageDto.getContent().get(1);
        Assertions.assertEquals("Alpha", s1.getName());
        Assertions.assertEquals("A1", s1.getLocationCode());
        Assertions.assertEquals("Beta", s2.getName());
        Assertions.assertEquals("B1", s2.getLocationCode());

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * For an empty page: repository returns no items, page metadata is copied, and API page content
     * is an empty list (not null).
     */
    @Test
    void getPage_empty_ok() {
        var pageable = PageRequest.of(1, 10);
        Page<NationalCourtHouse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAllActiveCourts(null, null, pageable)).thenReturn(emptyPage);

        PagingWrapper wrapper = PagingWrapper.of(List.of(), pageable);

        doAnswer(
                        inv -> {
                            CourtLocationPage out = inv.getArgument(1);
                            out.setTotalElements(0L);
                            out.setTotalPages(0);
                            out.setPageNumber(1);
                            out.setPageSize(10);
                            return null;
                        })
                .when(pageMapper)
                .toPage(
                        eq(emptyPage),
                        org.mockito.ArgumentMatchers.any(CourtLocationPage.class),
                        eq(wrapper.getSortStrings()));

        CourtLocationPage pageDto = service.getPage(null, null, wrapper);

        Assertions.assertEquals(0, pageDto.getTotalElements());
        Assertions.assertEquals(0, pageDto.getTotalPages());
        Assertions.assertEquals(1, pageDto.getPageNumber());
        Assertions.assertEquals(10, pageDto.getPageSize());
        Assertions.assertTrue(pageDto.getContent().isEmpty());

        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CourtLocationAuditOperation.GET_COURT_LOCATIONS_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }
}
