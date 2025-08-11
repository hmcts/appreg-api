package uk.gov.hmcts.appregister.applicationentry.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.appregister.applicationcode.model.ApplicationCode;
import uk.gov.hmcts.appregister.applicationlist.model.ApplicationList;
import uk.gov.hmcts.appregister.applicationresult.model.ApplicationResult;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicant;

@Entity
@Table(name = "application")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"applicationList", "result"})
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_list_id", nullable = false)
    private ApplicationList applicationList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_applicant_id")
    private StandardApplicant standardApplicant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_code_id", nullable = false)
    private ApplicationCode applicationCode;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "applicant_id")
    private IdentityDetails applicant;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "respondent_id")
    private IdentityDetails respondent;

    @Column(name = "number_of_bulk_respondents")
    private Integer numberOfBulkRespondents;

    @Column(name = "application_wording")
    private String applicationWording;

    @Column(name = "case_reference", length = 15)
    private String caseReference;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "application_rescheduled", length = 1)
    private String applicationRescheduled;

    @Column(name = "notes", length = 4000)
    private String notes;

    @Column(name = "bulk_upload")
    private String bulkUpload;

    @Builder.Default
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationFeeRecord> feeRecords = new ArrayList<>();

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private ApplicationResult result;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_date", nullable = false)
    private LocalDate changedDate;

    @Column(name = "version", nullable = false)
    private Integer version;

    public void addFeeRecord(ApplicationFeeRecord record) {
        if (record == null) {
            return;
        }
        this.feeRecords.add(record);
        record.setApplication(this);
    }
}
