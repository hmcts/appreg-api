package uk.gov.hmcts.appregister.resolutioncode.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity mapping to the {@code resolution_codes} table.
 *
 * <p>This class represents a single Result Code record in the database. It contains the full
 * metadata required for display in admin and ALE workflows, including wording, legislation,
 * destination email addresses, and validity dates.
 *
 * <p><strong>Lombok Annotations:</strong>
 *
 * <ul>
 *   <li>{@link Data} – generates getters, setters, equals/hashCode, and toString.
 *   <li>{@link Builder} – enables fluent builder API for constructing instances.
 *   <li>{@link NoArgsConstructor} / {@link AllArgsConstructor} – provide convenience constructors.
 * </ul>
 *
 * <p><strong>Database Annotations:</strong>
 *
 * <ul>
 *   <li>{@link Entity} – marks this as a JPA entity.
 *   <li>{@link Table} – maps the entity to the {@code resolution_codes} table.
 *   <li>{@link Column} – customises column mapping, constraints, and lengths.
 * </ul>
 */
@Entity
@Table(name = "resolution_code")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionCode {

    // Primary key identifier (maps to {@code rc_id}).
    @Id
    @Column(name = "rc_id", nullable = false, updatable = false)
    private Long id;

    // Short alphanumeric result code value (max length 10).
    @Column(name = "resolution_code", nullable = false, length = 10)
    private String resultCode;

    // Human-readable title for the result code (max length 500).
    @Column(name = "resolution_code_title", nullable = false, length = 500)
    private String title;

    // Full descriptive wording of the result code, often displayed in UIs.
    @Column(name = "resolution_code_wording", nullable = false)
    private String wording;

    // Optional legislation reference that this result code is associated with.
    @Column(name = "resolution_legislation")
    private String legislation;

    // Optional primary destination email address associated with this result code.
    @Column(name = "rc_destination_email_address_1")
    private String destinationEmail1;

    // Optional secondary destination email address associated with this result code.
    @Column(name = "rc_destination_email_address_2")
    private String destinationEmail2;

    // Start date (inclusive) from which this result code is valid.
    @Column(name = "resolution_code_start_date", nullable = false)
    private LocalDate startDate;

    // Optional end date (inclusive) until which this result code remains valid.
    @Column(name = "resolution_code_end_date")
    private LocalDate endDate;
}
