package uk.gov.hmcts.appregister.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "standard_applicant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardApplicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code")
    private String applicantCode;

    @Column(name = "title")
    private String applicantTitle;

    @Column(name = "name")
    private String applicantName;

    @Column(name = "forename_1")
    private String applicantForename1;

    @Column(name = "forename_2")
    private String applicantForename2;

    @Column(name = "forename_3")
    private String applicantForename3;

    @Column(name = "surname")
    private String applicantSurname;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "address_line_3")
    private String addressLine3;

    @Column(name = "address_line_4")
    private String addressLine4;

    @Column(name = "address_line_5")
    private String addressLine5;

    @Column(name = "postcode")
    private String postcode;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "telephone_number")
    private String telephoneNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "start_date")
    private LocalDate applicantStartDate;

    @Column(name = "end_date")
    private LocalDate applicantEndDate;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_date", nullable = false)
    private LocalDate changedDate;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "user_name", length = 250, nullable = false)
    private String userName;
}
