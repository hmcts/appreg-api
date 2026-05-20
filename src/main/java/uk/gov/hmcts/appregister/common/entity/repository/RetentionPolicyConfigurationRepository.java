package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.RetentionPolicyConfiguration;

@Repository
public interface RetentionPolicyConfigurationRepository
        extends JpaRepository<RetentionPolicyConfiguration, Long> {
    @Query(
            """
            select rpc.configValue
            from RetentionPolicyConfiguration rpc, RetentionPolicy rp, DatabaseJob dj
            where rpc.retentionPolicyId = rp.id
              and rp.databaseJobId = dj.id
              and dj.name = :jobName
              and rpc.configKey = :configKey
            """)
    Optional<String> findConfigValueByJobNameAndConfigKey(
            @Param("jobName") String jobName, @Param("configKey") String configKey);

    @Modifying
    @Query(
            """
            update RetentionPolicyConfiguration rpc
            set rpc.configValue = :configValue
            where rpc.configKey = :configKey
              and rpc.retentionPolicyId in (
                    select rp.id
                    from RetentionPolicy rp
                    where rp.databaseJobId in (
                        select dj.id from DatabaseJob dj where dj.name = :jobName
                    )
              )
            """)
    int updateConfigValueByJobNameAndConfigKey(
            @Param("jobName") String jobName,
            @Param("configKey") String configKey,
            @Param("configValue") String configValue);
}
