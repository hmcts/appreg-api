package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

@Service
@RequiredArgsConstructor
public class ApplicationListVersionService {

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void incrementVersion(ApplicationList applicationList) {
        entityManager.lock(applicationList, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void incrementVersions(Collection<ApplicationList> applicationLists) {
        var listsById = new TreeMap<Long, ApplicationList>();
        applicationLists.forEach(list -> listsById.putIfAbsent(list.getId(), list));
        listsById.values().forEach(this::incrementVersion);
    }
}
