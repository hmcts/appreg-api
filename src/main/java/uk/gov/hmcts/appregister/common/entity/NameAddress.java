package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;

/** Represents a Name and Address entity mapped to the "name_address" table in the database. */
@Entity
@Table(name = "name_address")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NameAddress extends BaseChangeableEntity implements Accountable {
    @Id
    @Column(name = "na_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "na_gen")
    @SequenceGenerator(name = "na_gen", sequenceName = "na_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "title")
    private String title;

    @Column(name = "forename_1")
    @Size(max = 100)
    private String forename1;

    @Column(name = "forename_2")
    @Size(max = 100)
    private String forename2;

    @Column(name = "forename_3")
    @Size(max = 100)
    private String forename3;

    @Column(name = "surname")
    private String surname;

    @Column(name = "address_l1")
    @Size(max = 35)
    private String address1;

    @Column(name = "address_l2")
    @Size(max = 35)
    private String address2;

    @Column(name = "address_l3")
    @Size(max = 35)
    private String address3;

    @Column(name = "address_l4")
    @Size(max = 35)
    private String address4;

    @Column(name = "address_l5")
    @Size(max = 35)
    private String address5;

    @Column(name = "postcode")
    @Size(max = 8)
    private String postcode;

    @Column(name = "email_address")
    @Size(max = 253)
    private String emailAddress;

    @Column(name = "telephone_number")
    @Size(max = 20)
    private String telephoneNumber;

    @Column(name = "mobile_number")
    @Size(max = 20)
    private String mobileNumber;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "user_name", nullable = false)
    @Size(max = 250)
    private String userName;

    @Column(name = "date_of_birth")
    private OffsetDateTime dateOfBirth;

    @Column(name = "dms_id")
    @Size(max = 20)
    private Long dmsId;

    @Override
    public String getCreatedUser() {
        return userName;
    }

    @Override
    public void setCreatedUser(String user) {
        this.userName = user;
    }
}
