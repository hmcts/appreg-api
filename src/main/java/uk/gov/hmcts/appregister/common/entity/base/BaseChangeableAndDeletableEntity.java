package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.converter.YesNoConverter;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(PreCreateUpdateEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BaseChangeableAndDeletableEntity implements Changeable, Deletable {
    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_date", nullable = false)
    private OffsetDateTime changedDate;

    @Column(name = "delete_by")
    private String deletedBy;

    @Column(name = "delete_date")
    private OffsetDateTime deletedDate;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "is_deleted")
    private YesOrNo deleted;
}
