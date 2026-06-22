package uk.gov.hmcts.appregister.applicationfee.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeServiceImpl;
import uk.gov.hmcts.appregister.applicationfee.service.mapper.FeeMapperImpl;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;

@ExtendWith(MockitoExtension.class)
class ApplicationFeeServiceImplTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2025-10-07T10:00:00Z");
    private static final LocalDate TODAY_UK = LocalDate.of(2025, Month.OCTOBER, 7);

    @Mock private FeeRepository repository;

    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private ApplicationFeeServiceImpl applicationFeeService;

    @Test
    void testMainAndOffsiteFee() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);

        Fee feeMain = new Fee();
        feeMain.setId(1L);
        feeMain.setOffsite(false);

        Fee feeOffsite = new Fee();
        feeOffsite.setId(2L);
        feeOffsite.setOffsite(true);

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull()))
                .thenReturn(List.of(feeMain));

        when(repository.findOffsite(notNull())).thenReturn(List.of(feeOffsite));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        assertEquals(feeMain, feePair.mainFee());
        assertEquals(feeOffsite, feePair.offsiteFee());
    }

    @Test
    void testMainAndNoOffsiteFee() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);

        Fee feeMain = new Fee();
        feeMain.setId(1L);
        feeMain.setOffsite(false);

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull()))
                .thenReturn(List.of(feeMain));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        assertEquals(feeMain, feePair.mainFee());
        assertNull(feePair.offsiteFee());
    }

    @Test
    void testOffsiteFeeAndNoMainFee() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);

        Fee feeOffsite = new Fee();
        feeOffsite.setId(1L);
        feeOffsite.setOffsite(true);

        String ref = "ref";
        when(repository.findOffsite(notNull())).thenReturn(List.of(feeOffsite));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        assertEquals(feeOffsite, feePair.offsiteFee());
        assertNull(feePair.mainFee());
    }

    @Test
    void testNoOffsiteFeeAndNoMainFee() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull())).thenReturn(List.of());

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        assertNull(feePair.offsiteFee());
        assertNull(feePair.mainFee());
    }

    @Test
    void testMultipleOffsiteFeeAndMainFee() {
        when(businessDateProvider.currentUkDate()).thenReturn(TODAY_UK);

        // generate multiple main and offsite fees
        Fee feeMain = new Fee();
        feeMain.setId(5L);
        feeMain.setOffsite(false);

        Fee feeMain2 = new Fee();
        feeMain2.setId(1L);
        feeMain2.setOffsite(false);

        Fee feeOffsite = new Fee();
        feeOffsite.setId(4L);
        feeOffsite.setOffsite(true);

        Fee feeOffsite2 = new Fee();
        feeOffsite2.setId(2L);
        feeOffsite2.setOffsite(true);

        String ref = "ref";

        when(repository.findByReferenceBetweenDate(eq(ref), notNull()))
                .thenReturn(List.of(feeMain, feeMain2));

        when(repository.findOffsite(notNull())).thenReturn(List.of(feeOffsite, feeOffsite2));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        assertEquals(feeMain, feePair.mainFee());
        assertEquals(feeOffsite, feePair.offsiteFee());
    }

    @Test
    void testResolveFeePairsBatchesReferencesAndKeepsFirstOrderedMatch() {
        var feeMain = new Fee();
        feeMain.setId(5L);
        feeMain.setReference("ref-one");
        feeMain.setOffsite(false);

        var feeMainOlder = new Fee();
        feeMainOlder.setId(1L);
        feeMainOlder.setReference("ref-one");
        feeMainOlder.setOffsite(false);

        var secondFee = new Fee();
        secondFee.setId(6L);
        secondFee.setReference("ref-two");
        secondFee.setOffsite(false);

        var feeOffsite = new Fee();
        feeOffsite.setId(9L);
        feeOffsite.setOffsite(true);

        when(repository.findByReferenceInBetweenDate(List.of("ref-one", "ref-two"), TODAY_UK))
                .thenReturn(List.of(feeMain, feeMainOlder, secondFee));
        when(repository.findOffsite(TODAY_UK)).thenReturn(List.of(feeOffsite));

        Map<String, FeePair> feePairs =
                applicationFeeService.resolveFeePairs(
                        List.of("REF-ONE", "REF-TWO", "REF-ONE"), TODAY_UK);

        var expected = new LinkedHashMap<String, FeePair>();
        expected.put("REF-ONE", new FeePair(feeMain, feeOffsite));
        expected.put("REF-TWO", new FeePair(secondFee, feeOffsite));
        assertEquals(expected, feePairs);
    }

    @Test
    void testUpsertFee_insert() {
        when(repository.findByReferenceBetweenDate("CO6.7", TODAY_UK)).thenReturn(List.of());

        val fee = new Fee();
        fee.setId(67L);
        fee.setReference("CO6.7");
        fee.setVersion(1L);
        fee.setOffsite(false);
        fee.setStartDate(LocalDate.now());
        fee.setDescription("Unit Test Fee");
        fee.setChangedBy(67L);
        fee.setChangedDate(OffsetDateTime.now());
        fee.setAmount(BigDecimal.valueOf(10.00));
        fee.setCreatedUser("Unit Test");
        fee.setEndDate(TODAY_UK);

        val listener = new CapturingAuditListener();
        val serviceImpl =
                new ApplicationFeeServiceImpl(
                        repository,
                        businessDateProvider,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener),
                        new FeeMapperImpl());

        serviceImpl.upsertFee(fee);

        verify(repository, times(1)).findByReferenceBetweenDate("CO6.7", TODAY_UK);
        verify(repository, times(1)).saveAndFlush(any(Fee.class));

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (Fee) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(fee.getReference(), audited.getReference());
        Assertions.assertEquals(fee.getAmount(), audited.getAmount());
        Assertions.assertEquals(fee.getChangedDate(), audited.getChangedDate());
        Assertions.assertEquals(fee.getStartDate(), audited.getStartDate());
        Assertions.assertEquals(fee.getEndDate(), audited.getEndDate());
    }

    @Test
    void testUpsertFee_update() {

        val existingFee = new Fee();
        existingFee.setId(67L);
        existingFee.setReference("CO6.0");
        existingFee.setVersion(1L);
        existingFee.setOffsite(false);
        existingFee.setStartDate(LocalDate.now());
        existingFee.setDescription("Unit Test Fee 2");
        existingFee.setChangedBy(66L);
        existingFee.setChangedDate(OffsetDateTime.now());
        existingFee.setAmount(BigDecimal.valueOf(10.00));
        existingFee.setCreatedUser("Unit Test 2");
        existingFee.setEndDate(TODAY_UK);

        when(repository.findByReferenceBetweenDate("CO6.7", TODAY_UK))
                .thenReturn(List.of(existingFee));

        val fee = new Fee();
        fee.setId(67L);
        fee.setReference("CO6.7");
        fee.setVersion(1L);
        fee.setOffsite(false);
        fee.setStartDate(LocalDate.now());
        fee.setDescription("Unit Test Fee");
        fee.setChangedBy(67L);
        fee.setChangedDate(OffsetDateTime.now());
        fee.setAmount(BigDecimal.valueOf(10.00));
        fee.setCreatedUser("Unit Test");
        fee.setEndDate(TODAY_UK);

        val listener = new CapturingAuditListener();
        val serviceImpl =
                new ApplicationFeeServiceImpl(
                        repository,
                        businessDateProvider,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener),
                        new FeeMapperImpl());

        serviceImpl.upsertFee(fee);

        verify(repository, times(1)).findByReferenceBetweenDate(fee.getReference(), TODAY_UK);
        verify(repository, times(1)).saveAndFlush(any(Fee.class));

        Assertions.assertNotNull(listener.getCompleteEvent());

        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (Fee) listener.getCompleteEvent().getNewValue();
        Assertions.assertEquals(fee.getReference(), audited.getReference());
        Assertions.assertEquals(fee.getAmount(), audited.getAmount());
        Assertions.assertEquals(fee.getChangedDate(), audited.getChangedDate());
        Assertions.assertEquals(fee.getStartDate(), audited.getStartDate());
        Assertions.assertEquals(fee.getEndDate(), audited.getEndDate());
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
}
