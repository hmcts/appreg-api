package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.audit.listener.diff.Audit;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditEnabled;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@Entity
@Table(name = TableNames.RETENTION_POLICY)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AuditEnabled(types = {CrudEnum.READ, CrudEnum.UPDATE})
public class RetentionPolicy implements Keyable {
    @Id
    @Column(name = "rp_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rp_gen")
    @SequenceGenerator(name = "rp_gen", sequenceName = "rp_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "dj_dj_id", nullable = false)
    @Audit(action = {CrudEnum.READ, CrudEnum.UPDATE})
    private Long databaseJobId;

    @Column(name = "config_key")
    @Audit(action = {CrudEnum.READ, CrudEnum.UPDATE})
    private String configKey;

    @Column(name = "config_value")
    @Audit(action = {CrudEnum.READ, CrudEnum.UPDATE})
    private String configValue;

    @Column(name = "config_notes")
    private String configNotes;

    @Override
    public Long getId() {
        return id;
    }
}
