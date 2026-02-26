package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;

@Repository
public interface AppListEntrySequenceMappingRepository
    extends JpaRepository<AppListEntrySequenceMapping, Long> {

    /**
     * Atomically allocate the next sequence number for the given application list id.
     *
     * <p>This executes a single statement that:
     * <ol>
     *   <li>inserts a mapping row with sequence = 1 if it does not exist, or</li>
     *   <li>increments ale_last_sequence by 1 if it does</li>
     * </ol>
     *
     * <p>The statement returns the new ale_last_sequence value.
     *
     * @param alId the application list id (al_id)
     * @return the allocated sequence number (ale_last_sequence) as Integer
     */
    @Query(
        value =
            "WITH next_seq AS ( " +
                "  INSERT INTO al_ale_sequence_mapping (al_id, ale_last_sequence) " +
                "  VALUES (:alId, 1) " +
                "  ON CONFLICT (al_id) DO UPDATE " +
                "    SET ale_last_sequence = functionalschema.al_ale_sequence_mapping.ale_last_sequence + 1 " +
                "  RETURNING ale_last_sequence " +
                ") SELECT ale_last_sequence FROM next_seq",
        nativeQuery = true
    )
    Integer allocateNextSequence(@Param("alId") Long alId);
}
