package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = TableNames.ASYNC_JOBS_APP_LIST_ENTRY)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AsyncJobsAppListEntry {

    @Id
    @Column(name = "aj_ale_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aj_ale_gen")
    @SequenceGenerator(name = "aj_ale_gen", sequenceName = "aj_ale_seq", allocationSize = 1)
    private Long id;

    @Column(name = "aj_id", nullable = false, updatable = false)
    private UUID asyncJobId;

    @Column(name = "ale_id", nullable = false, updatable = false)
    private UUID appListEntryId;
}
