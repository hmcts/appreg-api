package uk.gov.hmcts.appregister.common.concurrency;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@ExtendWith(MockitoExtension.class)
public class MatchServiceImplTest {

    @Mock private MatchProvider matchRequest;

    @InjectMocks public MatchServiceImpl matchService;

    @Test
    public void testProcessMatchSuccess() {
        UUID id = UUID.randomUUID();
        Versionable versionable = mock(Versionable.class);
        when(versionable.getVersion()).thenReturn(1L);
        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(id, versionable, payload);

        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        matchService.matchOnRequest(
                id,
                versionable,
                () -> {
                    ;
                    return matchResponse;
                });
    }

    @Test
    void testProcessMatchFailOnVersion() {
        UUID id = UUID.randomUUID();
        Versionable versionable = mock(Versionable.class);
        when(versionable.getVersion()).thenReturn(1L);
        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(id, versionable, payload);

        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        // change the version to simulate a conflict
        Versionable versionable1 = mock(Versionable.class);
        when(versionable1.getVersion()).thenReturn(2L);

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                matchService.matchOnRequest(
                                        id,
                                        versionable1,
                                        () -> {
                                            ;

                                            return MatchResponse.of(id, versionable1, "test");
                                        }));
        Assertions.assertEquals(CommonAppError.MATCH_ETAG_FAILURE, exception.getCode());
    }

    @Test
    void testProcessMatchFailOnId() {
        UUID id = UUID.randomUUID();
        Versionable versionable = mock(Versionable.class);
        when(versionable.getVersion()).thenReturn(1L);
        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(id, versionable, payload);

        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        // change the id to simulate a conflict
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                matchService.matchOnRequest(
                                        UUID.randomUUID(),
                                        versionable,
                                        () -> MatchResponse.of(id, versionable, payload)));
        Assertions.assertEquals(CommonAppError.MATCH_ETAG_FAILURE, exception.getCode());
    }

    @Test
    void testProcessMatchFailOnType() {
        UUID id = UUID.randomUUID();
        Versionable versionable = mock(Versionable.class);
        when(versionable.getVersion()).thenReturn(1L);
        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(id, versionable, payload);

        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        // change the id to simulate a conflict
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                matchService.matchOnRequest(
                                        id,
                                        new ApplicationList(),
                                        () -> MatchResponse.of(id, versionable, payload)));
        Assertions.assertEquals(CommonAppError.MATCH_ETAG_FAILURE, exception.getCode());
    }
}
