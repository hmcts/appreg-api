package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.audit.listener.diff.AuditEnabled;
import uk.gov.hmcts.appregister.common.entity.base.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Mapping table that holds the last allocated sequence number for each application list (al_id).
 */
@Entity
@Table(name = TableNames.APPLICATION_LIST_ENTRY_SEQUENCE_MAPPING)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AuditEnabled(types = {CrudEnum.CREATE, CrudEnum.UPDATE})
public class AppListEntrySequenceMapping {

    @Id
    @Column(name = "al_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @NotNull
    private Long alId;

    @Column(name = "ale_last_sequence", nullable = false)
    private Integer aleLastSequence;
}
