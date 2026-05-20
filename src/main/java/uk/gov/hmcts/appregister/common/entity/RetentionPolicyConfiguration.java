package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = TableNames.RETENTION_POLICY_CONFIGURATION)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RetentionPolicyConfiguration {
    @Id
    @Column(name = "rpc_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rpc_gen")
    @SequenceGenerator(name = "rpc_gen", sequenceName = "rpc_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "rp_rp_id", nullable = false)
    private Long retentionPolicyId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "config_notes")
    private String configNotes;
}
