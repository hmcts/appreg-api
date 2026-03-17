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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.base.TableNames;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * Represents an Address entity mapped to the "addresses" table in the database.
 */
@Entity
@Table(name = TableNames.ADDRESSES)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Address extends BaseUnmanagedChangeableEntity implements Versionable, Keyable {
    @Id
    @Column(name = "adr_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adr_gen")
    @SequenceGenerator(name = "adr_gen", sequenceName = "adr_id", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "line1")
    @Size(max = 35)
    private String line1;

    @Column(name = "line2")
    @Size(max = 35)
    private String line2;

    @Column(name = "line3")
    @Size(max = 35)
    private String line3;

    @Column(name = "line4")
    @Size(max = 35)
    private String line4;

    @Column(name = "line5")
    @Size(max = 35)
    private String line5;

    @Column(name = "postcode")
    private String postcode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "version_number", nullable = false)
    @Version
    private Long version;

    @Column(name = "mcc_mcc_id")
    private Long cja;
}
