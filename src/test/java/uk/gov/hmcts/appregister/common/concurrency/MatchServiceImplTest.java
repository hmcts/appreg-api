package uk.gov.hmcts.appregister.common.concurrency;

import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@ExtendWith(MockitoExtension.class)
public class MatchServiceImplTest {

    @Mock private HttpMatchProviderImpl matchRequest;

    @InjectMocks public MatchServiceImpl matchService;

    @Test
    public void testProcessMatchSuccess() {
        DummyKeyableThatIsVersionable versionable = new DummyKeyableThatIsVersionable(1L);
        versionable.setVersion(1L);

        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(payload, List.of(versionable));

        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        matchService.matchOnRequest(
                () -> {
                    return matchResponse;
                },
                List.of(versionable));
    }

    @Test
    void testProcessMatchFailOnVersion() {
        DummyKeyableThatIsVersionable versionable = new DummyKeyableThatIsVersionable(1L);
        versionable.setVersion(0L);

        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(payload, List.of(versionable));
        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        // change the version to simulate a conflict
        DummyKeyableThatIsVersionable versionable1 = new DummyKeyableThatIsVersionable(1L);
        versionable1.setVersion(1L);

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                matchService.matchOnRequest(
                                        () -> {
                                            return MatchResponse.of("test", List.of(versionable));
                                        },
                                        List.of(versionable1)));
        Assertions.assertEquals(CommonAppError.MATCH_ETAG_FAILURE, exception.getCode());
    }

    @Test
    void testProcessMatchFailOnId() {
        DummyKeyableThatIsVersionable versionable = new DummyKeyableThatIsVersionable(1L);
        String payload = "payload";

        MatchResponse<String> matchResponse = MatchResponse.of(payload, List.of(versionable));

        DummyKeyableThatIsVersionable versionable1 = new DummyKeyableThatIsVersionable(2L);

        when(matchRequest.getEtag()).thenReturn(matchResponse.getEtag());

        // change the id to simulate a conflict
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                matchService.matchOnRequest(
                                        () -> MatchResponse.of(payload, List.of(versionable1)),
                                        List.of(versionable1)));
        Assertions.assertEquals(CommonAppError.MATCH_ETAG_FAILURE, exception.getCode());
    }

    class DummyKeyable implements Keyable {
        private final Long id;

        public DummyKeyable(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    class DummyKeyableThatIsVersionable implements Keyable, Versionable {
        private final Long id;
        private Long version = 0L;

        public DummyKeyableThatIsVersionable(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setVersion(Long version) {
            this.version = version;
        }

        @Override
        public Long getVersion() {
            return version;
        }
    }
}
