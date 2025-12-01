package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.time.LocalTime;
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
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

/**
 * The ApplicationList entity represents a list of applications in the system.
 */
@Entity
@Table(name = TableNames.APPLICATION_LISTS)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuppressWarnings("javaarchitecture:S7027")
@AuditEnabled(types = {CrudEnum.CREATE, CrudEnum.UPDATE})
public class ApplicationList extends BaseChangeableAndDeletableEntity
        implements Accountable, Versionable, Keyable {
    @Id
    @Column(name = "al_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "al_gen")
    @SequenceGenerator(name = "al_gen", sequenceName = "al_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    @Audit(action = {CrudEnum.UPDATE})
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(name = "id", insertable = false, updatable = false, columnDefinition = "uuid")
    private java.util.UUID uuid;

    @Column(name = "application_list_status")
    @Enumerated(EnumType.STRING)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private ApplicationListStatus status;

    @Column(name = "list_description", nullable = false)
    @Size(max = 200)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private String description;

    @Column(name = "courthouse_name")
    @Size(max = 200)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private String courtName;

    @Column(name = "courthouse_code")
    @Size(max = 10)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private String courtCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cja_cja_id")
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private CriminalJusticeArea cja;

    @Column(name = "other_courthouse")
    @Size(max = 200)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private String otherLocation;

    @Column(name = "application_list_date", nullable = false)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private LocalDate date;

    @Column(name = "application_list_time", nullable = false)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE})
    private LocalTime time;

    @Column(name = "duration_hour")
    @Audit(action = {CrudEnum.UPDATE})
    private short durationHours;

    @Column(name = "duration_minute")
    @Audit(action = {CrudEnum.UPDATE})
    private short durationMinutes;

    @Column(name = "user_name")
    @Size(max = 250)
    private String createdUser;

    @Column(name = "version")
    @Version
    @Audit(action = {CrudEnum.UPDATE})
    private Long version;
}
