package uk.gov.hmcts.appregister.applicationlist.service;

import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

@ExtendWith(MockitoExtension.class)
class ApplicationListVersionServiceTest {

    @Mock private EntityManager entityManager;

    @InjectMocks private ApplicationListVersionService service;

    @Test
    void given_applicationList_when_incrementVersion_then_forceIncrementVersion() {
        var applicationList = mock(ApplicationList.class);

        service.incrementVersion(applicationList);

        verify(entityManager).lock(applicationList, OPTIMISTIC_FORCE_INCREMENT);
    }

    @Test
    void given_duplicateUnorderedLists_when_incrementVersions_then_incrementEachOnceById() {
        var lowerIdList = applicationList(1L);
        var higherIdList = applicationList(2L);

        service.incrementVersions(List.of(higherIdList, lowerIdList, higherIdList));

        var ordered = inOrder(entityManager);
        ordered.verify(entityManager).lock(lowerIdList, OPTIMISTIC_FORCE_INCREMENT);
        ordered.verify(entityManager).lock(higherIdList, OPTIMISTIC_FORCE_INCREMENT);
        ordered.verifyNoMoreInteractions();
    }

    private ApplicationList applicationList(long id) {
        var applicationList = new ApplicationList();
        applicationList.setId(id);
        return applicationList;
    }
}
