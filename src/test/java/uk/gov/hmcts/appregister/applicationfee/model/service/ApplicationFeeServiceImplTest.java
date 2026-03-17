package uk.gov.hmcts.appregister.applicationfee.model.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeServiceImpl;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.base.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;

@ExtendWith(MockitoExtension.class)
public class ApplicationFeeServiceImplTest {

    @Mock private FeeRepository repository;

    @Mock private Clock clock;

    @InjectMocks private ApplicationFeeServiceImpl applicationFeeService;

    @Test
    public void testMainAndOffsiteFee() {

        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        Fee feeMain = new Fee();
        feeMain.setId(1L);
        feeMain.setOffsite(false);

        Fee feeOffsite = new Fee();
        feeOffsite.setId(2L);
        feeOffsite.setOffsite(true);

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull()))
                .thenReturn(List.of(feeMain, feeOffsite));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        Assertions.assertEquals(feeMain, feePair.mainFee());
        Assertions.assertEquals(feeOffsite, feePair.offsiteFee());
    }

    @Test
    public void testMainAndNoOffsiteFee() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        Fee feeMain = new Fee();
        feeMain.setId(1L);
        feeMain.setOffsite(false);

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull()))
                .thenReturn(List.of(feeMain));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        Assertions.assertEquals(feeMain, feePair.mainFee());
        Assertions.assertNull(feePair.offsiteFee());
    }

    @Test
    public void testOffsiteFeeAndNoMainFee() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        Fee feeOffsite = new Fee();
        feeOffsite.setId(1L);
        feeOffsite.setOffsite(true);

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull()))
                .thenReturn(List.of(feeOffsite));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        Assertions.assertEquals(feeOffsite, feePair.offsiteFee());
        Assertions.assertNull(feePair.mainFee());
    }

    @Test
    public void testNoOffsiteFeeAndNoMainFee() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        String ref = "ref";
        when(repository.findByReferenceBetweenDate(eq(ref), notNull())).thenReturn(List.of());

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        Assertions.assertNull(feePair.offsiteFee());
        Assertions.assertNull(feePair.mainFee());
    }

    @Test
    public void testMultipleOffsiteFeeAndMainFee() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

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
                .thenReturn(List.of(feeMain, feeMain2, feeOffsite, feeOffsite2));

        // test
        FeePair feePair = applicationFeeService.resolveFeePair(ref);

        // assert
        Assertions.assertEquals(feeMain, feePair.mainFee());
        Assertions.assertEquals(feeOffsite, feePair.offsiteFee());
    }
}
