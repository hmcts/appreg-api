package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Entity for Data Audit table. */
@Entity
@Table(name = "data_audit")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DataAudit {
  @Id
  @Column(name = "aler_id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "add_dataaudit_event_gen")
  @SequenceGenerator(
      name = "add_dataaudit_event_gen",
      sequenceName = "add_dataaudit_event",
      allocationSize = 1)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(name = "schema", nullable = false)
  private String schema;

  @Column(name = "schema_name", nullable = false)
  private String schemaName;

  @Column(name = "table_name", nullable = false)
  private String tableName;

  @Column(name = "column_name", nullable = false)
  private String columnName;

  @Column(name = "old_value")
  private String oldValue;

  @Column(name = "new_value")
  private String newValue;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "link")
  private String link;

  @Column(name = "created_date", nullable = false)
  private OffsetDateTime createdDate;

  @Column(name = "old_clob_value")
  private String oldClobValue;

  @Column(name = "related_key")
  private Long relatedKey;

  @Column(name = "update_type", nullable = false)
  private String updateType;

  @Column(name = "data_type")
  private String dataType;

  @Column(name = "case_id")
  private Long caseId;

  @Column(name = "related_items_identifier")
  private String relatedItemsIdentifier;

  @Column(name = "related_items_identifier_index")
  private String relatedItemsIdentifierIndex;

  @Column(name = "event_name")
  private String eventName;

  @Column(name = "user_name")
  private String userName;
}
