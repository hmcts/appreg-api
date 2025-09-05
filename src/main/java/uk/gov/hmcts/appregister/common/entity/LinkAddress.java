package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

/** Represents a LinkAddress entity mapped to the "link_addresses" table in the database. */
@Entity
@Table(name = "link_addresses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LinkAddress extends BaseChangeableEntity implements Versionable {
  @Id
  @Column(name = "la_id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "la_gen")
  @SequenceGenerator(name = "la_gen", sequenceName = "la_seq", allocationSize = 1)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(name = "no_fixed_abode", nullable = false)
  private String noFixedAbode;

  @Column(name = "la_type", nullable = false)
  private String laType;

  @Column(name = "start_date", nullable = false)
  private OffsetDateTime startDate;

  @Column(name = "end_date")
  private OffsetDateTime endDate;

  @Column(name = "version_number", nullable = false)
  private Long version;

  @Column(name = "head_office_indicator")
  @Size(max = 1)
  private String headOfficeIndicator;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "adr_adr_id")
  private Address addresses;

  @Column(name = "bu_bu_id")
  private Long buId;

  @Column(name = "er_er_id")
  private Long erId;

  @Column(name = "loc_loc_id")
  private Long locId;
}
