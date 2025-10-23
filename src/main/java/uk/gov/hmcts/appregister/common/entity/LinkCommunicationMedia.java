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
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * Represents a LinkCommunicationMedia entity mapped to the "link_communication_media" table in the
 * database.
 */
@Entity
@Table(name = TableNames.LINK_COMMUNICATION_MEDIA)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
public class LinkCommunicationMedia extends BaseUnmanagedChangeableEntity implements Versionable {

    @Id
    @Column(name = "lcm_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lcm_gen")
    @SequenceGenerator(name = "lcm_gen", sequenceName = "lcm_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "lcm_type", nullable = false)
    @Size(max = 2)
    private String lcmType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "version_number", nullable = false)
    @Size(max = 38)
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comm_comm_id")
    private CommunicationMedia commId;

    @Column(name = "loc_loc_id")
    private Long locId;

    @Column(name = "er_er_id")
    private Long erId;

    @Column(name = "bu_bu_id")
    private Long buId;
}
