package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * The ApplicationList entity represents a list of applications in the system.
 */
@Entity
@Table(name = "application_lists")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuppressWarnings("javaarchitecture:S7027")
public class ApplicationList extends BaseChangeableEntity implements Accountable, Versionable {
    @Id
    @Column(name = "al_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "al_gen")
    @SequenceGenerator(name = "al_gen", sequenceName = "al_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long pk;

    @Generated(event = EventType.INSERT)
    @Column(name = "id", insertable = false, updatable = false, columnDefinition = "uuid")
    private java.util.UUID uuid;

    @Column(name = "application_list_status")
    @Size(max = 6)
    private String status;

    @Column(name = "list_description", nullable = false)
    @Size(max = 200)
    private String description;

    @Column(name = "courthouse_name")
    @Size(max = 200)
    private String courtName;

    @Column(name = "courthouse_code")
    @Size(max = 10)
    private String courtCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cja_cja_id")
    private CriminalJusticeArea cja;

    @Column(name = "other_courthouse")
    @Size(max = 200)
    private String otherLocation;

    @Column(name = "application_list_date", nullable = false)
    private LocalDate date;

    @Column(name = "application_list_time", nullable = false)
    private LocalTime time;

    @Column(name = "duration_hour")
    private short durationHours;

    @Column(name = "duration_minute")
    private short durationMinutes;

    @Column(name = "user_name")
    @Size(max = 250)
    private String createdUser;

    @Column(name = "version")
    @Version
    private Long version;
}
