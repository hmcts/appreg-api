package uk.gov.hmcts.appregister.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_fee")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference")
    private String reference;

    @Column(name = "description", nullable = false, length = 250)
    private String description;

    @Column(name = "amount", nullable = false, precision = 9, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_offset", nullable = false)
    private boolean isOffset;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "version")
    private Integer version;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_date")
    private LocalDate changedDate;

    @Column(name = "user_name")
    private String userName;
}
