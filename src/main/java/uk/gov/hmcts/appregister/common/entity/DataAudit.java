package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;

/** Entity for Data Audit table. */
@Entity
@Table(name = "data_audit")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
public class DataAudit implements Accountable {
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
    private String createdUser;

    @Column(name = "link")
    @Size(max = 100)
    private String link;

    @Column(name = "created_date", nullable = false)
    private OffsetDateTime createdDate;

    @Column(name = "old_clob_value")
    private String oldClobValue;

    @Column(name = "related_key")
    private BigDecimal relatedKey;

    @Column(name = "update_type", nullable = false)
    @Size(max = 1)
    private String updateType;

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
    @Size(max = 20)
    private String userName;
}
