package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
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
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * The AppListEntryResolution entity represents a resolution entry for an application list entry.
 */
@Entity
@Table(name = "app_list_entry_resolutions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@EntityListeners(PreCreateUpdateEntityListener.class)
@AuditEnabled(types = {CrudEnum.DELETE, CrudEnum.CREATE, CrudEnum.UPDATE})
public class AppListEntryResolution extends BaseChangeableEntity
        implements Accountable, Versionable, Keyable {

    @Id
    @Column(name = "aler_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aler_gen")
    @SequenceGenerator(name = "aler_gen", sequenceName = "aler_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    @Audit(action = {CrudEnum.DELETE, CrudEnum.CREATE})
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ale_ale_id", nullable = false)
    @Audit(action = {CrudEnum.CREATE})
    private ApplicationListEntry applicationList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rc_rc_id", nullable = false)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private ResolutionCode resolutionCode;

    @Column(name = "al_entry_resolution_wording", nullable = false)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private String resolutionWording;

    @Column(name = "al_entry_resolution_officer", nullable = false)
    @Size(max = 1000)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private String resolutionOfficer;

    @Column(name = "version", nullable = false)
    @Version
    @Audit(action = {CrudEnum.DELETE, CrudEnum.CREATE, CrudEnum.UPDATE})
    private Long version;

    @Column(name = "user_name")
    @Size(max = 250)
    private String createdUser;

    @Generated(event = EventType.INSERT)
    @Column(name = "id", insertable = false, updatable = false, columnDefinition = "uuid")
    private java.util.UUID uuid;
}
