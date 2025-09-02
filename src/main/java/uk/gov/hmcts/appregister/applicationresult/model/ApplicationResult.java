package uk.gov.hmcts.appregister.applicationresult.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.appregister.applicationentry.model.Application;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;

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
    private ResolutionCode resultCode;

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
