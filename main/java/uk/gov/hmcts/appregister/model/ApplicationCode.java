package uk.gov.hmcts.appregister.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * No XSD so we had to map the data using
 * APPLICATION_CODE table in SYSTEM.APPREGISTER
 * from Oracle DB.
 */

@Entity
@Table(name = "application_code")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationCode {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "application_code")
    private String applicationCode;

    @Column(name = "title")
    private String title;

    @Column(name = "wording")
    private String wording;

    @Column(name = "legislation")
    private String legislation;

    @Column(name = "fee_due")
    private Boolean feeDue;

    @Column(name = "requires_respondent")
    private Boolean requiresRespondent;

    @Column(name = "destination_email_address_1")
    private String destinationEmail1;

    @Column(name = "destination_email_address_2")
    private String destinationEmail2;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "bulk_respondent_allowed")
    private Boolean bulkRespondentAllowed;

    @Column(name = "fee_reference")
    private String feeReference;
}
