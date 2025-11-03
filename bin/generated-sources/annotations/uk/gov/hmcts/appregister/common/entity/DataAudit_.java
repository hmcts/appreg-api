package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@StaticMetamodel(DataAudit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class DataAudit_ {

	public static final String NEW_VALUE = "newValue";
	public static final String DATA_TYPE = "dataType";
	public static final String LINK = "link";
	public static final String RELATED_ITEMS_IDENTIFIER_INDEX = "relatedItemsIdentifierIndex";
	public static final String RELATED_KEY = "relatedKey";
	public static final String SCHEMA_NAME = "schemaName";
	public static final String USER_ID = "userId";
	public static final String TABLE_NAME = "tableName";
	public static final String RELATED_ITEMS_IDENTIFIER = "relatedItemsIdentifier";
	public static final String CHANGED_DATE = "changedDate";
	public static final String CASE_ID = "caseId";
	public static final String EVENT_NAME = "eventName";
	public static final String ID = "id";
	public static final String OLD_VALUE = "oldValue";
	public static final String OLD_CLOB_VALUE = "oldClobValue";
	public static final String CREATED_USER = "createdUser";
	public static final String COLUMN_NAME = "columnName";
	public static final String UPDATE_TYPE = "updateType";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#newValue
	 **/
	public static volatile SingularAttribute<DataAudit, String> newValue;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#dataType
	 **/
	public static volatile SingularAttribute<DataAudit, String> dataType;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#link
	 **/
	public static volatile SingularAttribute<DataAudit, String> link;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#relatedItemsIdentifierIndex
	 **/
	public static volatile SingularAttribute<DataAudit, String> relatedItemsIdentifierIndex;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#relatedKey
	 **/
	public static volatile SingularAttribute<DataAudit, BigDecimal> relatedKey;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#schemaName
	 **/
	public static volatile SingularAttribute<DataAudit, String> schemaName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#userId
	 **/
	public static volatile SingularAttribute<DataAudit, String> userId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#tableName
	 **/
	public static volatile SingularAttribute<DataAudit, String> tableName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#relatedItemsIdentifier
	 **/
	public static volatile SingularAttribute<DataAudit, String> relatedItemsIdentifier;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#changedDate
	 **/
	public static volatile SingularAttribute<DataAudit, OffsetDateTime> changedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#caseId
	 **/
	public static volatile SingularAttribute<DataAudit, BigDecimal> caseId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#eventName
	 **/
	public static volatile SingularAttribute<DataAudit, String> eventName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#id
	 **/
	public static volatile SingularAttribute<DataAudit, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#oldValue
	 **/
	public static volatile SingularAttribute<DataAudit, String> oldValue;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#oldClobValue
	 **/
	public static volatile SingularAttribute<DataAudit, String> oldClobValue;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit
	 **/
	public static volatile EntityType<DataAudit> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#createdUser
	 **/
	public static volatile SingularAttribute<DataAudit, String> createdUser;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#columnName
	 **/
	public static volatile SingularAttribute<DataAudit, String> columnName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.DataAudit#updateType
	 **/
	public static volatile SingularAttribute<DataAudit, String> updateType;

}

