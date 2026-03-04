package uk.gov.hmcts.appregister.common.entity.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;

@Repository
public interface AppListEntrySequenceMappingRepository
        extends JpaRepository<AppListEntrySequenceMapping, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM AppListEntrySequenceMapping m WHERE m.alId = :alId")
    Optional<AppListEntrySequenceMapping> findByAlIdForUpdate(@Param("alId") Long alId);
}
