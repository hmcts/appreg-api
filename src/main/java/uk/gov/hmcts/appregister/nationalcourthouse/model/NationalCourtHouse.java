package uk.gov.hmcts.appregister.nationalcourthouse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;

/**
 * Entity model representing a row in the {@code national_court_houses} table.
 *
 * <p>This maps directly to the schema used in PSS/SDRS so that JPA can read/write records without
 * custom SQL. Column names have been kept consistent with the underlying schema for ease of mapping
 * and reference.
 *
 * <p>Notes from source system:
 *
 * <ul>
 *   <li><strong>NCH</strong> = National Court House
 *   <li><strong>SL_NAME</strong> = Court House Welsh name
 *   <li>Some codes (e.g. HOCODE, MCC) appear in the schema but their meaning is specific to Libra
 *       and may not be fully documented.
 * </ul>
 *
 * <p>This class is typically mapped into a {@link
 * NationalCourtHouseDto} by the {@link
 * NationalCourtHouseMapper} for API exposure.
 */
@Entity
@Table(name = "national_court_house")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NationalCourtHouse {

    // Primary key identifier for the court location.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nch_id", nullable = false, updatable = false)
    private Long id;

    // Name of the courthouse (e.g. "Cardiff Crown Court").
    @Column(name = "courthouse_name", nullable = false)
    private String name;

    // Type of court, such as "CROWN" or "MAGISTRATES".
    @Column(name = "court_type", nullable = false)
    private String courtType;

    // Date when this court record became effective. Always required.
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Date when this court record ended, or {@code null} if still active.
    @Column(name = "end_date")
    private LocalDate endDate;

    // Foreign key reference to the location record (LOC).
    @Column(name = "loc_loc_id")
    private Long locationId;

    // Foreign key reference to the petty sessional area (PSA).
    @Column(name = "psa_psa_id")
    private Long psaId;

    // Business reference code for this court location (used in integration).
    @Column(name = "court_location_code")
    private String courtLocationCode;

    // Welsh-language name for the courthouse, if available.
    @Column(name = "sl_courthouse_name")
    private String welshName;

    // Organisation identifier (NORG) linking this court to its parent org.
    @Column(name = "norg_id")
    private Long orgId;
}
