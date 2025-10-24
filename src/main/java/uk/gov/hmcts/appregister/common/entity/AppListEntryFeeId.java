package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.entity.compositeid.AppListEntryFeeCompositeId;

/**
 * Represents the association between an application list entry and a fee, mapped to the
 * "app_list_entry_fee_id" table in the database.
 */
@Entity
@Table(name = "app_list_entry_fee_id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AppListEntryFeeCompositeId.class)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@Setter
public class AppListEntryFeeId extends BaseChangeableEntity implements Accountable, Versionable {
    @Id
    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ale_ale_id", nullable = false)
    private ApplicationListEntry appListEntryId;

    @Id
    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_fee_id", nullable = false)
    private Fee feeId;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "user_name", nullable = false)
    @Size(max = 250)
    private String createdUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appListEntryId", nullable = false)
    private ApplicationListEntry le;
}
