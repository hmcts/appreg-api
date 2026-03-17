package uk.gov.hmcts.appregister.resultcode.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.mapper.SortableFieldMapper;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.resultcode.audit.ResultCodeAuditOperation;
import uk.gov.hmcts.appregister.resultcode.exception.ResultCodeError;
import uk.gov.hmcts.appregister.resultcode.mapper.ResultCodeMapper;

@ExtendWith(MockitoExtension.class)
public class ResultCodeServiceImplTest {

    @Mock private ResolutionCodeRepository repository;
    @Mock private ResultCodeMapper mapper;
    @Mock private PageMapper pageMapper;

    @Spy
    private List<AuditOperationLifecycleListener> auditListeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService =
            new AuditOperationServiceImpl(new ObjectMapper(), auditListeners);

    private ResultCodeServiceImpl service;

    @BeforeEach
    public void setup() {
        ZoneId ukZone = ZoneId.of("Europe/London");
        Clock fixedClock = Clock.fixed(Instant.parse("2024-10-05T10:15:30Z"), ZoneId.of("UTC"));

        service =
                new ResultCodeServiceImpl(
                        auditOperationService,
                        auditListeners,
                        repository,
                        mapper,
                        pageMapper,
                        fixedClock,
                        ukZone);
    }

    /**
     * Given exactly one active result code row for (code,date), the service maps it to a detail DTO
     * and returns 200-style data.
     */
    @Test
    void findByCode_success_singleRow() {
        final String code = "RC123";
        final LocalDate date = LocalDate.parse("2025-01-01");

        var entity = new ResolutionCode();
        var expectedDto = new ResultCodeGetDetailDto();

        String wordingTemplate = "Appeal forwarded to {TEXT|Name of Crown Court|100}.";
        entity.setWording(wordingTemplate);

        when(repository.findActiveResolutionCodesByCodeAndDate(eq(code), any()))
                .thenReturn(List.of(entity));
        when(mapper.toDetailDto(entity)).thenReturn(expectedDto);

        ResultCodeGetDetailDto actual = service.findByCode(code, date);
        Assertions.assertSame(expectedDto, actual);

        verify(auditOperationService)
                .processAudit(
                        eq(ResultCodeAuditOperation.GET_RESULT_CODE_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * When the repository returns no rows for (code,date), the service throws AppRegistryException
     * with RESULT_CODE_NOT_FOUND.
     */
    @Test
    void findByCode_notFound_throws() {
        final String code = "MISSING";
        final LocalDate date = LocalDate.parse("2025-01-01");
        when(repository.findActiveResolutionCodesByCodeAndDate(eq(code), any()))
                .thenReturn(List.of());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.findByCode(code, date));
        Assertions.assertEquals(ResultCodeError.RESULT_CODE_NOT_FOUND, ex.getCode());

        verify(auditOperationService)
                .processAudit(
                        eq(ResultCodeAuditOperation.GET_RESULT_CODE_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * When multiple rows exist for (code,date), the service throws AppRegistryException with
     * DUPLICATE_RESULT_CODE_FOUND.
     */
    @Test
    void findByCode_duplicate_throws() {
        final String code = "DUP";
        final LocalDate date = LocalDate.parse("2025-01-01");

        var r1 = new ResolutionCode();
        var r2 = new ResolutionCode();
        when(repository.findActiveResolutionCodesByCodeAndDate(eq(code), any()))
                .thenReturn(List.of(r1, r2));

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> service.findByCode(code, date));
        Assertions.assertEquals(ResultCodeError.DUPLICATE_RESULT_CODE_FOUND, ex.getCode());

        verify(auditOperationService)
                .processAudit(
                        eq(ResultCodeAuditOperation.GET_RESULT_CODE_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * For a non-empty page: repository page is mapped to API page, page metadata is copied, and
     * each entity becomes a summary DTO (we assert size to avoid relying on field names).
     */
    @Test
    void findAll_success_mapsContentAndMeta() {
        final String codeFilter = "R";
        final String titleFilter = "X";
        final var pageable = PageRequest.of(0, 2);
        ;

        var e1 = new ResolutionCode();
        var e2 = new ResolutionCode();
        Page<ResolutionCode> dbPage = new PageImpl<>(List.of(e1, e2), pageable, 5);

        when(repository.findActiveOnDate(eq(codeFilter), eq(titleFilter), any(), eq(pageable)))
                .thenReturn(dbPage);

        PagingWrapper wrapper = PagingWrapper.of(SortableFieldMapper.of("id,asc"), pageable);
        // Simulate page meta copy so assertions have values
        doAnswer(
                        inv -> {
                            ResultCodePage out = inv.getArgument(1);
                            out.setTotalElements(dbPage.getTotalElements());
                            out.setTotalPages(dbPage.getTotalPages());
                            out.setPageNumber(dbPage.getNumber());
                            out.setPageSize(dbPage.getSize());
                            return null;
                        })
                .when(pageMapper)
                .toPage(eq(dbPage), any(ResultCodePage.class), eq(wrapper.getSortStrings()));

        ResultCodePage pageDto = service.findAll(codeFilter, titleFilter, wrapper);

        Assertions.assertEquals(5, pageDto.getTotalElements());
        Assertions.assertEquals(3, pageDto.getTotalPages());
        Assertions.assertEquals(0, pageDto.getPageNumber());
        Assertions.assertEquals(2, pageDto.getPageSize());
        Assertions.assertEquals(2, pageDto.getContent().size());

        verify(auditOperationService)
                .processAudit(
                        eq(ResultCodeAuditOperation.GET_RESULT_CODES_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }

    /**
     * For an empty page: repository returns no items, page metadata is copied, and API page content
     * is an empty list (not null).
     */
    @Test
    void findAll_empty_ok() {
        final var pageable = PageRequest.of(1, 10);

        Page<ResolutionCode> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(repository.findActiveOnDate(eq(null), eq(null), any(), eq(pageable)))
                .thenReturn(emptyPage);
        PagingWrapper wrapper = PagingWrapper.of(SortableFieldMapper.of("id,asc"), pageable);
        doAnswer(
                        inv -> {
                            ResultCodePage out = inv.getArgument(1);
                            out.setTotalElements(0L);
                            out.setTotalPages(0);
                            out.setPageNumber(1);
                            out.setPageSize(10);
                            return null;
                        })
                .when(pageMapper)
                .toPage(eq(emptyPage), any(ResultCodePage.class), eq(wrapper.getSortStrings()));

        ResultCodePage pageDto = service.findAll(null, null, wrapper);

        Assertions.assertEquals(0, pageDto.getTotalElements());
        Assertions.assertEquals(0, pageDto.getTotalPages());
        Assertions.assertEquals(1, pageDto.getPageNumber());
        Assertions.assertEquals(10, pageDto.getPageSize());
        Assertions.assertTrue(pageDto.getContent().isEmpty());

        verify(auditOperationService)
                .processAudit(
                        eq(ResultCodeAuditOperation.GET_RESULT_CODES_AUDIT_EVENT),
                        notNull(),
                        notNull());
    }
}
