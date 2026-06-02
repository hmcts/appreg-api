package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.RetentionPolicy;

@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, Long> {
    @Query(
            """
            select rp
            from RetentionPolicy rp, DatabaseJob dj
            where rp.databaseJobId = dj.id
              and dj.name = :jobName
              and rp.configKey = :configKey
            order by rp.id asc
            """)
    List<RetentionPolicy> findByJobNameAndConfigKeyOrderByIdAsc(
            @Param("jobName") String jobName, @Param("configKey") String configKey);

    @Query(
            """
            select count(rp)
            from RetentionPolicy rp, DatabaseJob dj
            where rp.databaseJobId = dj.id
              and dj.name = :jobName
              and rp.configKey = :configKey
            """)
    long countByJobNameAndConfigKey(
            @Param("jobName") String jobName, @Param("configKey") String configKey);
}
