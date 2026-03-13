package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.audit.listener.diff.Audit;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditEnabled;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Represents a StandardApplicant entity mapped to the "standard_applicants" table in the database.
 */
@Entity
@Table(name = TableNames.STANDARD_APPLICANTS)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@AuditEnabled(types = {CrudEnum.READ})
public class StandardApplicant extends BaseUnmanagedChangeableEntity
        implements Accountable, Versionable, Keyable {
    @Id
    @Column(name = "sa_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sa_gen")
    @SequenceGenerator(name = "sa_gen", sequenceName = "sa_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "standard_applicant_code", nullable = false)
    @Audit(action = {CrudEnum.READ})
    @Pattern(
            regexp = "[a-zA-Z0-9\\-\\+\\._ ]*",
            message =
                    "Applicant code must be alphanumeric and can include"
                            + " hyphens, plus signs, dots, underscores, and spaces")
    private String applicantCode;

    @Column(name = "standard_applicant_start_date", nullable = false)
    @Audit(action = {CrudEnum.READ})
    private LocalDate applicantStartDate;

    @Column(name = "standard_applicant_end_date")
    private LocalDate applicantEndDate;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "user_name", length = 250, nullable = false)
    private String createdUser;

    @Column(name = "name", length = 100)
    @Audit(action = {CrudEnum.READ})
    private String name;

    @Column(name = "title", length = 100)
    private String applicantTitle;

    @Column(name = "forename_1", length = 100)
    private String applicantForename1;

    @Column(name = "forename_2", length = 100)
    private String applicantForename2;

    @Column(name = "forename_3", length = 100)
    private String applicantForename3;

    @Column(name = "surname", length = 100)
    private String applicantSurname;

    @Column(name = "address_l1", length = 35, nullable = false)
    private String addressLine1;

    @Column(name = "address_l2", length = 35)
    private String addressLine2;

    @Column(name = "address_l3", length = 35)
    private String addressLine3;

    @Column(name = "address_l4", length = 35)
    private String addressLine4;

    @Column(name = "address_l5", length = 35)
    private String addressLine5;

    @Column(name = "postcode", length = 8)
    private String postcode;

    @Column(name = "email_address", length = 253)
    private String emailAddress;

    @Column(name = "telephone_number", length = 20)
    private String telephoneNumber;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    /**
     * determine if an applicant is an organisation.
     *
     * @param applicant An applicant
     * @return True or false
     */
    public static boolean isOrganisation(StandardApplicant applicant) {
        return applicant.getName() != null && !applicant.getName().isBlank();
    }
}
