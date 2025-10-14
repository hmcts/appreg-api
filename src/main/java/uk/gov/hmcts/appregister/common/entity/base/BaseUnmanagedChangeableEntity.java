package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BaseUnmanagedChangeableEntity implements UnmanagedChangeable {
    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_date", nullable = false)
    private OffsetDateTime changedDate;
}
