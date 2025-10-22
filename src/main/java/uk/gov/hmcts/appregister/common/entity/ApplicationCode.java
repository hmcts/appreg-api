package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/*
 * No XSD so we had to map the data using
 * APPLICATION_CODE table in SYSTEM.APPREGISTER
 * from Oracle DB.
 */

/**
 * Represents an ApplicationCode entity mapped to the "application_codes" table in the database.
 */
@Entity
@Table(name = TableNames.APPLICATION_CODES)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@Setter
public class ApplicationCode extends BaseUnmanagedChangeableEntity
        implements Accountable, Versionable {

    @Id
    @Column(name = "ac_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ac_gen")
    @SequenceGenerator(name = "ac_gen", sequenceName = "ac_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "application_code", nullable = false)
    @Size(max = 10)
    private String code;

    @Column(name = "application_code_title", nullable = false)
    @Size(max = 500)
    private String title;

    @Column(name = "application_code_wording", nullable = false)
    private String wording;

    @Column(name = "application_legislation")
    private String legislation;

    @Column(name = "fee_due", nullable = false)
    @Size(max = 1)
    private String feeDue;

    @Column(name = "application_code_respondent", nullable = false)
    @Size(max = 1)
    private String requiresRespondent;

    @Column(name = "ac_destination_email_address_1")
    @Size(max = 553)
    private String destinationEmail1;

    @Column(name = "ac_destination_email_address_2")
    @Size(max = 500)
    private String destinationEmail2;

    @Column(name = "application_code_start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "application_code_end_date")
    private OffsetDateTime endDate;

    @Column(name = "bulk_respondent_allowed", nullable = false)
    @Size(max = 1)
    private String bulkRespondentAllowed;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "ac_fee_reference")
    private String feeReference;

    @Override
    public String getCreatedUser() {
        return userName;
    }

    @Override
    public void setCreatedUser(String user) {
        userName = user;
    }

    @OneToMany(mappedBy = "applicationCode")
    @Builder.Default
    private List<ApplicationListEntry> applicationListEntryList = new ArrayList<>();
}
