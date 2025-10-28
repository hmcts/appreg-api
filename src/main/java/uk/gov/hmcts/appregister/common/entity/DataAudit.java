package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.PreCreateUpdateEntityListener;
import uk.gov.hmcts.appregister.common.entity.converter.CrudConverter;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

/**
 * Entity for Data Audit table.
 */
@Entity
@Table(name = "data_audit")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@EntityListeners(PreCreateUpdateEntityListener.class)
public class DataAudit implements Changeable {
    @Id
    @Column(name = "data_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "add_dataaudit_event_gen")
    @SequenceGenerator(
            name = "add_dataaudit_event_gen",
            sequenceName = "add_dataaudit_event",
            allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "schema_name", nullable = false)
    @Size(max = 30)
    private String schemaName;

    @Column(name = "table_name", nullable = false)
    @Size(max = 30)
    private String tableName;

    @Column(name = "column_name", nullable = false)
    @Size(max = 30)
    private String columnName;

    @Column(name = "old_value")
    @Size(max = 4000)
    private String oldValue;

    @Column(name = "new_value")
    @Size(max = 4000)
    private String newValue;

    @Column(name = "user_id")
    @Size(max = 32)
    private String userId;

    @Column(name = "link")
    @Size(max = 100)
    private String link;

    @Column(name = "created_date", nullable = false)
    private OffsetDateTime changedDate;

    @Column(name = "old_clob_value")
    private String oldClobValue;

    @Column(name = "related_key")
    private Long relatedKey;

    @Column(name = "update_type", nullable = false)
    @Convert(converter = CrudConverter.class)
    private CRUDEnum updateType;

    @Column(name = "data_type")
    @Size(max = 1000)
    private String dataType;

    @Column(name = "case_id")
    private BigDecimal caseId;

    @Column(name = "related_items_identifier")
    @Size(max = 30)
    private String relatedItemsIdentifier;

    @Column(name = "related_items_identifier_index")
    @Size(max = 30)
    private String relatedItemsIdentifierIndex;

    @Column(name = "event_name")
    @Size(max = 100)
    private String eventName;

    @Column(name = "user_name")
    @Size(max = 250)
    private String changedBy;
}
