package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;
import uk.gov.hmcts.appregister.common.entity.base.UnmanagedChangeable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * Represents a Fee entity mapped to the "fee" table in the database.
 */
@Entity
@Table(name = TableNames.FEE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(PreCreateUpdateEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Fee implements Accountable, UnmanagedChangeable, Versionable {

    @Id
    @Column(name = "fee_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fee_gen")
    @SequenceGenerator(name = "fee_gen", sequenceName = "fee_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fee_reference", nullable = false, length = 12)
    private String reference;

    @Column(name = "fee_description", nullable = false, length = 250)
    private String description;

    @Column(name = "fee_value", nullable = false)
    private BigDecimal amount;

    @Column(name = "fee_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "fee_end_date")
    private LocalDate endDate;

    @Column(name = "fee_version", nullable = false)
    @Version
    private Long version;

    @Column(name = "fee_changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "fee_changed_date", nullable = false)
    private OffsetDateTime changedDate;

    @Column(name = "fee_user_name", nullable = false)
    private String createdUser;

    @Column(name = "is_offsite")
    private boolean isOffsite;
}
