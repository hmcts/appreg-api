package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * JPA entity representing a row in the {@code national_court_house} table.
 *
 * <p>This model provides the persistence-layer mapping for National Court Houses, aligning with the
 * legacy PSS/SDRS schema so JPA can read and write records without custom SQL. Column names are
 * kept consistent with the database for easier reference.
 *
 * <p><strong>Usage:</strong>
 *
 * <ul>
 *   <li>Loaded and persisted via Spring Data repositories. responses.
 *   <li>Consumed in paginated search results returned directly as Spring Data {@link
 *       org.springframework.data.domain.Page}.
 * </ul>
 *
 * <p><strong>Notes from source system:</strong>
 *
 * <ul>
 *   <li><strong>NCH</strong> = National Court House
 *   <li><strong>SL_NAME</strong> = Court House Welsh name
 *   <li>Some legacy codes (e.g. HOCODE, MCC) appear in the schema but are not directly modelled
 *       here.
 * </ul>
 */
@Entity
@Table(name = "national_court_houses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NationalCourtHouse extends BaseUnmanagedChangeableEntity implements Versionable {

    // Primary key identifier for the courthouse record.
    @Id
    @Column(name = "nch_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nch_gen")
    @SequenceGenerator(name = "nch_gen", sequenceName = "nch_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    // Name of the courthouse (e.g. "Cardiff Crown Court").
    @Column(name = "courthouse_name", nullable = false)
    private String name;

    @Column(name = "version_number", nullable = false)
    @Version
    private Long version;

    @Column(name = "court_type", nullable = false)
    private String courtType;

    // Date when this record became effective. Always required.
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Date when this record ended, or {@code null} if still active.
    @Column(name = "end_date")
    private LocalDate endDate;

    // Foreign key reference to a linked location record.
    @Column(name = "loc_loc_id")
    private Long locationId;

    // Foreign key reference to the petty sessions area (PSA).
    @ManyToOne()
    @JoinColumn(name = "psa_psa_id", nullable = false)
    private PettySessionalArea psaId;

    // Business reference code for this court location, used in integrations.
    @Column(name = "court_location_code")
    private String courtLocationCode;

    // Welsh-language name for the courthouse, if available.
    @Column(name = "sl_courthouse_name")
    private String welshName;

    // Organisation identifier linking this court to its parent organisation.
    @Column(name = "norg_id")
    private Long orgId;
}
