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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

/**
 * Represents an entry in the application list, mapped to the "application_list_entries" table in
 * the database.
 */
@Entity
@Table(name = TableNames.APPLICATION_LISTS_ENTRY)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuppressWarnings("javaarchitecture:S7027")
@AuditEnabled(types = {CrudEnum.CREATE, CrudEnum.UPDATE, CrudEnum.READ})
public class ApplicationListEntry extends BaseChangeableAndDeletableEntity
        implements Accountable, Versionable, Keyable {

    @Id
    @Column(name = "ale_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ale_gen")
    @SequenceGenerator(name = "ale_gen", sequenceName = "ale_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE, CrudEnum.READ})
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "al_al_id")
    @Audit(action = {CrudEnum.CREATE, CrudEnum.UPDATE, CrudEnum.READ})
    private ApplicationList applicationList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sa_sa_id")
    private StandardApplicant standardApplicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ac_ac_id", nullable = false)
    private ApplicationCode applicationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "a_na_id")
    @Audit(action = {CrudEnum.READ})
    private NameAddress anamedaddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "r_na_id")
    @Audit(action = {CrudEnum.READ})
    private NameAddress rnameaddress;

    @Column(name = "number_of_bulk_respondents")
    private Short numberOfBulkRespondents;

    @Column(name = "application_list_entry_wording", nullable = false)
    private String applicationListEntryWording;

    @Column(name = "case_reference")
    @Size(max = 15)
    private String caseReference;

    @Column(name = "account_number")
    @Size(max = 20)
    @Audit(action = {CrudEnum.READ})
    private String accountNumber;

    @Column(name = "entry_rescheduled", nullable = false)
    @Size(max = 1)
    private String entryRescheduled;

    @Column(name = "notes")
    @Size(max = 4000)
    private String notes;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "bulk_upload")
    private String bulkUpload;

    @Column(name = "user_name")
    private String createdUser;

    @Column(name = "sequence_number", nullable = false)
    private Short sequenceNumber;

    @Column(name = "tcep_status")
    private String tcepStatus;

    @Column(name = "message_uuid")
    private String messageUuid;

    @Column(name = "retry_count")
    private String retryCount;

    @Column(name = "lodgement_date", nullable = false)
    private LocalDate lodgementDate;

    @OneToMany(mappedBy = "applicationList")
    private List<AppListEntryResolution> resolutions;

    @OneToMany(mappedBy = "appListEntry")
    private List<AppListEntryOfficial> officials;

    @OneToMany(mappedBy = "appListEntry")
    private List<AppListEntryFeeStatus> entryFeeStatuses;

    @OneToMany(mappedBy = "appListEntryId")
    private List<AppListEntryFeeId> entryFeeIds;

    @Column(name = "id")
    @Generated(event = EventType.INSERT)
    @Audit(action = {CrudEnum.READ})
    private UUID uuid;
}
