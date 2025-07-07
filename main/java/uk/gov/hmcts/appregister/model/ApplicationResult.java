package uk.gov.hmcts.appregister.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "application_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "application")
@Builder
public class ApplicationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "result_code_id", nullable = false)
    private ResultCode resultCode;

    @Column(name = "result_wording")
    private String resultWording;

    @Column(name = "result_officer")
    private String resultOfficer;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_date", nullable = false)
    private LocalDate changedDate;

    @Column(name = "version", nullable = false)
    private Integer version;
}
