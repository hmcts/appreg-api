package uk.gov.hmcts.appregister.resolutioncode.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a row in the {@code resolution_code} table.
 *
 * <p>This entity holds the full metadata for a Resolution (Result) Code, including its wording,
 * legislation references, destination email addresses, and validity dates. It is the
 * persistence-layer model that maps directly to the database schema and should not be exposed
 * directly in API responses. For API exposure, use {@link
 * uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto} or {@link
 * uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto} created via a mapper
 * component.
 *
 * <p><strong>Lombok annotations:</strong>
 *
 * <ul>
 *   <li>{@link Data} – generates getters, setters, equals/hashCode, and toString methods.
 *   <li>{@link Builder} – enables a fluent builder API for constructing instances.
 *   <li>{@link NoArgsConstructor} and {@link AllArgsConstructor} – provide convenience
 *       constructors.
 * </ul>
 *
 * <p><strong>Database mapping:</strong>
 *
 * <ul>
 *   <li>{@link Entity} – marks this as a JPA-managed entity.
 *   <li>{@link Table} – maps this entity to the {@code resolution_code} table.
 *   <li>{@link Column} – defines explicit column mappings, constraints, and lengths.
 * </ul>
 */
@Entity
@Table(name = "resolution_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionCode {

    // Primary key identifier (maps to {@code rc_id}).
    @Id
    @Column(name = "rc_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "na_seq")
    @SequenceGenerator(name = "na_seq", sequenceName = "na_seq", allocationSize = 1)
    private Long id;

    // Short alphanumeric resolution code value (max length 10, e.g. "RC123").
    @Column(name = "resolution_code", nullable = false, length = 10)
    private String resultCode;

    // Human-readable title for the resolution code (max length 500).
    @Column(name = "resolution_code_title", nullable = false, length = 500)
    private String title;

    // Full descriptive wording of the resolution code, displayed in UIs and reports.
    @Column(name = "resolution_code_wording", nullable = false)
    private String wording;

    // Optional legislation reference associated with this resolution code.
    @Column(name = "resolution_legislation")
    private String legislation;

    /** Optional primary destination email address tied to this resolution code. */
    @Column(name = "rc_destination_email_address_1")
    private String destinationEmail1;

    // Optional secondary destination email address tied to this resolution code.
    @Column(name = "rc_destination_email_address_2")
    private String destinationEmail2;

    // Start date (inclusive) from which this resolution code is valid.
    @Column(name = "resolution_code_start_date", nullable = false)
    private LocalDate startDate;

    // End date (inclusive) until which this resolution code remains valid, or {@code null} if
    // ongoing.
    @Column(name = "resolution_code_end_date")
    private LocalDate endDate;
}
