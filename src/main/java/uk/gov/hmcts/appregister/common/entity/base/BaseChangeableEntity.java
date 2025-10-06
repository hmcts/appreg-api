package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Define a standard class for the changed state. Simple extend this class if you need to apply
 * these columns to an entity
 */
@MappedSuperclass
@Getter
@Setter
@EntityListeners(PreCreateUpdateEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BaseChangeableEntity implements Changeable {

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_date", nullable = false)
    private OffsetDateTime changedDate;
}
