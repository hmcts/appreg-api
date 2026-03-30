package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.converter.YesNoConverter;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

@Entity
@Table(name = TableNames.DATABASE_JOBS)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class DatabaseJob implements Keyable {
    @Id
    @Column(name = "dj_id", nullable = false, updatable = false, insertable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String name;

    @Column(name = "job_enabled", nullable = false)
    @Convert(converter = YesNoConverter.class)
    private YesOrNo enabled;

    @Column(name = "job_last_ran")
    private OffsetDateTime lastRan;

    @Override
    public Long getId() {
        return id;
    }
}
