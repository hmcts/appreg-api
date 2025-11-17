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
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * The AppListEntryFeeStatus entity represents the fee status of an application list entry.
 */
@Entity
@Table(name = "app_list_entry_fee_status")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(PreCreateUpdateEntityListener.class)
public class AppListEntryFeeStatus implements Changeable, Accountable, Versionable, Keyable {
    @Id
    @Column(name = "alefs_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alefs_gen")
    @SequenceGenerator(name = "alefs_gen", sequenceName = "alefs_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alefs_ale_id", nullable = false)
    private ApplicationListEntry appListEntry;

    @Column(name = "alefs_payment_reference")
    private String alefsPaymentReference;

    @Column(name = "alefs_fee_status")
    private String alefsFeeStatus;

    @Column(name = "alefs_fee_status_date", nullable = false)
    private OffsetDateTime alefsFeeStatusDate;

    @Column(name = "alefs_version", nullable = false)
    @Version
    private Long version;

    @Column(name = "alefs_changed_by", nullable = false)
    private String changedBy;

    @Column(name = "fee_changed_date", nullable = false)
    private OffsetDateTime changedDate;

    @Column(name = "alefs_user_name", nullable = false)
    private String createdUser;

    @Column(name = "alefs_status_creation_date", nullable = false)
    private OffsetDateTime alefsStatusCreationDate;
}
