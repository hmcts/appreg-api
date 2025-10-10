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
import uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;

/**
 * Represents a Communication Media entity mapped to the "communication_media" table in the
 * database.
 */
@Entity
@Table(name = "communication_media")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
public class CommunicationMedia extends BaseUnmanagedChangeableEntity implements Versionable {
    @Id
    @Column(name = "comm_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comm_gen")
    @SequenceGenerator(name = "comm_gen", sequenceName = "comm_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "detail", nullable = false)
    @Size(max = 254)
    private String detail;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Column(name = "version_number", nullable = false)
    @Size(max = 38)
    @Version
    private Long version;
}
