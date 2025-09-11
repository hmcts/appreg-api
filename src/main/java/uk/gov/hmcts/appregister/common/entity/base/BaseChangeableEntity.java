package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;

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
    private BigDecimal changedBy;

    @CreatedBy
    @UpdateTimestamp
    @Column(name = "changed_date", nullable = false)
    private OffsetDateTime changedDate;
}
