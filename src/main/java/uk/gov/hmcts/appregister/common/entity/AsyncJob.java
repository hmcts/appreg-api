package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import uk.gov.hmcts.appregister.audit.listener.diff.Audit;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditEnabled;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;

/**
 * An asynchronous job. As opposed to a polling job see {@link DatabaseJob}
 */
@Entity
@Table(name = TableNames.ASYNC_JOBS)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EntityListeners(PreCreateUpdateEntityListener.class)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@AuditEnabled(types = {CrudEnum.READ})
public class AsyncJob implements Changeable, Keyable {
    @Id
    @Column(name = "aj_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aj_gen")
    @SequenceGenerator(name = "aj_gen", sequenceName = "aj_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(name = "id", insertable = false, updatable = false, columnDefinition = "uuid")
    @Audit(action = {CrudEnum.READ})
    private java.util.UUID uuid;

    @Column(name = "job_state")
    private JobStatusType jobState;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "last_updated")
    private OffsetDateTime updateTime;

    @Column(name = "failure_message")
    private String failureMessage;

    @Column(name = "user_name")
    private String userName;

    @Override
    public String getChangedBy() {
        return userName;
    }

    @Override
    public OffsetDateTime getChangedDate() {
        return updateTime;
    }

    @Override
    public void setChangedBy(String changedBy) {
        this.userName = changedBy;
    }

    @Override
    public void setChangedDate(OffsetDateTime changedDate) {
        this.updateTime = changedDate;
    }
}
