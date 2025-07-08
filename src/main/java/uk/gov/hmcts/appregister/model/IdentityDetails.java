package uk.gov.hmcts.appregister.model;

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

@Entity
@Table(name = "identity_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // TODO: We could make this an enum, AP for Applicant, RE for Respondent.
    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "title")
    private String title;

    @Column(name = "forename_1")
    private String forename1;

    @Column(name = "forename_2")
    private String forename2;

    @Column(name = "forename_3")
    private String forename3;

    @Column(name = "surname")
    private String surname;

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

    // Optional: only if you plan to use them
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}
