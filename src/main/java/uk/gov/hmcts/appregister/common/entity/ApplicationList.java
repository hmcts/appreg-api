package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/** The ApplicationList entity represents a list of applications in the system. */
@Entity
@Table(name = "application_lists")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApplicationList extends BaseChangeableEntity implements Accountable, Versionable {
    @Id
    @Column(name = "al_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "al_gen")
    @SequenceGenerator(name = "al_gen", sequenceName = "al_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "application_list_status")
    @Size(max = 6)
    private String status;

    @Column(name = "application_list_date", nullable = false)
    private OffsetDateTime date;

    @Column(name = "application_list_time", nullable = false)
    private OffsetDateTime time;

    @Column(name = "courthouse_code")
    @Size(max = 10)
    private String courthouseCode;

    @Column(name = "other_courthouse")
    @Size(max = 200)
    private String description;

    @Column(name = "list_description", nullable = false)
    @Size(max = 200)
    private LocalDate listDescription;

    @Column(name = "user_name")
    @Size(max = 250)
    private String userName;

    @Column(name = "courthouse_name")
    @Size(max = 200)
    private String courthouseName;

    @Column(name = "version")
    private Long version;

    @Column(name = "duration_hour")
    private short durationHour;

    @Column(name = "duration_minute")
    private short durationMinute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cja_cja_id")
    private CriminalJusticeArea cja;

    @OneToMany(mappedBy = "applicationList")
    private List<ApplicationListEntry> entries;

    @Override
    public String getCreatedUser() {
        return userName;
    }

    @Override
    public void setCreatedUser(String user) {
        this.userName = user;
    }

    @OneToMany(mappedBy = "applicationList")
    private List<ApplicationRegister> registers;
}
