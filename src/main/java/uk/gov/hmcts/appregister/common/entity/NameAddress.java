package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.constraint.ValidNameAddress;
import uk.gov.hmcts.appregister.common.entity.converter.NameAddressConverter;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;

/**
 * Represents a Name and Address entity mapped to the "name_address" table in the database.
 */
@Entity
@Table(name = "name_address")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@AuditEnabled(types = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.READ})
@ValidNameAddress
public class NameAddress extends BaseChangeableEntity implements Accountable, Keyable {

    @Id
    @Column(name = "na_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "na_gen")
    @SequenceGenerator(name = "na_gen", sequenceName = "na_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private Long id;

    @Column(name = "code")
    @Convert(converter = NameAddressConverter.class)
    @NotNull
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private NameAddressCodeType code;

    @Column(name = "name")
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.READ})
    private String name;

    @Column(name = "title")
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.READ})
    private String title;

    @Column(name = "first_name")
    @Size(max = 100)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String firstName;

    @Column(name = "middle_name")
    @Size(max = 100)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String middleName;

    @Column(name = "last_name")
    @Size(max = 100)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.READ})
    private String lastName;

    @Column(name = "address_l1")
    @Size(max = 35)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String address1;

    @Column(name = "address_l2")
    @Size(max = 35)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String address2;

    @Column(name = "address_l3")
    @Size(max = 35)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String address3;

    @Column(name = "address_l4")
    @Size(max = 35)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String address4;

    @Column(name = "address_l5")
    @Size(max = 35)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String address5;

    @Column(name = "postcode")
    @Size(max = 8)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE, CrudEnum.READ})
    private String postcode;

    @Column(name = "email_address")
    @Size(max = 253)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String emailAddress;

    @Column(name = "telephone_number")
    @Size(max = 20)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String telephoneNumber;

    @Column(name = "mobile_number")
    @Size(max = 20)
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private String mobileNumber;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "user_name", nullable = false)
    @Size(max = 250)
    private String userName;

    @Column(name = "date_of_birth")
    @Audit(action = {CrudEnum.CREATE, CrudEnum.DELETE})
    private LocalDate dateOfBirth;

    @Column(name = "dms_id")
    @Size(max = 20)
    private String dmsId;

    @Override
    public String getCreatedUser() {
        return userName;
    }

    public String getForename1() {
        return firstName;
    }

    public void setForename1(String forename1) {
        this.firstName = forename1;
    }

    public String getForename2() {
        if (middleName == null) {
            return null;
        }
        String[] parts = middleName.split("\\s+", 2);
        return parts[0];
    }

    public void setForename2(String forename2) {
        String third = getForename3();
        this.middleName = joinMiddleNames(forename2, third);
    }

    public String getForename3() {
        if (middleName == null) {
            return null;
        }
        String[] parts = middleName.split("\\s+", 2);
        return parts.length > 1 ? parts[1] : null;
    }

    public void setForename3(String forename3) {
        String second = getForename2();
        this.middleName = joinMiddleNames(second, forename3);
    }

    public String getSurname() {
        return lastName;
    }

    public void setSurname(String surname) {
        this.lastName = surname;
    }

    private static String joinMiddleNames(String forename2, String forename3) {
        if (forename2 == null || forename2.isBlank()) {
            return (forename3 == null || forename3.isBlank()) ? null : forename3;
        }
        if (forename3 == null || forename3.isBlank()) {
            return forename2;
        }
        return forename2 + " " + forename3;
    }

    public static class NameAddressBuilder {
        private String forename1;
        private String forename2;
        private String forename3;
        private String surname;

        public NameAddressBuilder forename1(String forename1) {
            this.forename1 = forename1;
            this.firstName = forename1;
            return this;
        }

        public NameAddressBuilder forename2(String forename2) {
            this.forename2 = forename2;
            this.middleName = joinMiddleNames(forename2, this.forename3);
            return this;
        }

        public NameAddressBuilder forename3(String forename3) {
            this.forename3 = forename3;
            this.middleName = joinMiddleNames(this.forename2, forename3);
            return this;
        }

        public NameAddressBuilder surname(String surname) {
            this.surname = surname;
            this.lastName = surname;
            return this;
        }
    }

    @Override
    public void setCreatedUser(String user) {
        this.userName = user;
    }

    public boolean isApplicant() {
        return code != null && code == NameAddressCodeType.APPLICANT;
    }

    public boolean isRespondent() {
        return code != null && code == NameAddressCodeType.RESPONDENT;
    }
}
