package uk.gov.hmcts.appregister.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "application_fee_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationFeeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", length = 50)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FeeStatusType feeStatus;

    @Column(name = "status_date", nullable = false)
    private LocalDate statusDate;

    @Column(name = "creation_date")
    @CreationTimestamp // TODO: Could use this for all created dates.
    private OffsetDateTime creationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_fee_id", nullable = false)
    private ApplicationFee applicationFee;

    @Column(name = "version")
    private Integer version;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_date")
    private LocalDate changedDate;

    @Column(name = "user_name")
    private String userName;
}
