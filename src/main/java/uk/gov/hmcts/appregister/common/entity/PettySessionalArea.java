package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * Represents a Petty Sessional Area entity mapped to the "petty_sessional_areas" table in the
 * database.
 */
@Entity
@Table(name = "petty_sessional_areas")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PettySessionalArea extends BaseChangeableEntity implements Versionable {
  @Id
  @Column(name = "psa_id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "psa_gen")
  @SequenceGenerator(name = "psa_gen", sequenceName = "psa_seq", allocationSize = 1)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(name = "psa_name")
  @Size(max = 100)
  private String name;

  @Column(name = "short_name")
  @Size(max = 10)
  private String shortName;

  @Column(name = "version_number", nullable = false)
  @Size(max = 38)
  private Long version;

  @Column(name = "cma_cma_id")
  private Long cmaId;

  @Column(name = "psa_code", nullable = false)
  @Size(max = 4)
  private String psaCode;

  @Column(name = "start_date")
  private OffsetDateTime startDate;

  @Column(name = "end_date")
  private OffsetDateTime endDate;

  @Column(name = "jc_name")
  @Size(max = 200)
  private String jcName;

  @Column(name = "court_type", nullable = false)
  @Size(max = 10)
  private String courtType;

  @Column(name = "crime_cases_loc_id")
  private Long crimeCasesLocId;

  @Column(name = "fine_accounts_loc_id")
  private Long fineAccountsLocId;

  @Column(name = "maintenance_enforcement_loc_id")
  private Long enforcedLocId;

  @Column(name = "family_cases_loc_id")
  private Long familyCasesLocId;

  @Column(name = "court_location_code")
  private String courtLocationCode;

  @Column(name = "central_finance_loc_id")
  private Long centralFinanceLocId;

  @Column(name = "sl_psa_name")
  private String psaName;

  @Column(name = "norg_id")
  private Long orgId;
}
