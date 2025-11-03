package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(DataAudit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _DataAudit {

	String RELATED_ITEMS_IDENTIFIER = "relatedItemsIdentifier";
	String RELATED_ITEMS_IDENTIFIER_INDEX = "relatedItemsIdentifierIndex";
	String NEW_VALUE = "newValue";
	String CASE_ID = "caseId";
	String DATA_TYPE = "dataType";
	String TABLE_NAME = "tableName";
	String CREATED_USER = "createdUser";
	String RELATED_KEY = "relatedKey";
	String LINK = "link";
	String SCHEMA_NAME = "schemaName";
	String UPDATE_TYPE = "updateType";
	String ID = "id";
	String EVENT_NAME = "eventName";
	String COLUMN_NAME = "columnName";
	String CHANGED_DATE = "changedDate";
	String OLD_CLOB_VALUE = "oldClobValue";
	String USER_ID = "userId";
	String OLD_VALUE = "oldValue";

	
	/**
	 * @see DataAudit#relatedItemsIdentifier
	 **/
	TextAttribute<DataAudit> relatedItemsIdentifier = new TextAttributeRecord<>(RELATED_ITEMS_IDENTIFIER);
	
	/**
	 * @see DataAudit#relatedItemsIdentifierIndex
	 **/
	TextAttribute<DataAudit> relatedItemsIdentifierIndex = new TextAttributeRecord<>(RELATED_ITEMS_IDENTIFIER_INDEX);
	
	/**
	 * @see DataAudit#newValue
	 **/
	TextAttribute<DataAudit> newValue = new TextAttributeRecord<>(NEW_VALUE);
	
	/**
	 * @see DataAudit#caseId
	 **/
	SortableAttribute<DataAudit> caseId = new SortableAttributeRecord<>(CASE_ID);
	
	/**
	 * @see DataAudit#dataType
	 **/
	TextAttribute<DataAudit> dataType = new TextAttributeRecord<>(DATA_TYPE);
	
	/**
	 * @see DataAudit#tableName
	 **/
	TextAttribute<DataAudit> tableName = new TextAttributeRecord<>(TABLE_NAME);
	
	/**
	 * @see DataAudit#createdUser
	 **/
	TextAttribute<DataAudit> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see DataAudit#relatedKey
	 **/
	SortableAttribute<DataAudit> relatedKey = new SortableAttributeRecord<>(RELATED_KEY);
	
	/**
	 * @see DataAudit#link
	 **/
	TextAttribute<DataAudit> link = new TextAttributeRecord<>(LINK);
	
	/**
	 * @see DataAudit#schemaName
	 **/
	TextAttribute<DataAudit> schemaName = new TextAttributeRecord<>(SCHEMA_NAME);
	
	/**
	 * @see DataAudit#updateType
	 **/
	TextAttribute<DataAudit> updateType = new TextAttributeRecord<>(UPDATE_TYPE);
	
	/**
	 * @see DataAudit#id
	 **/
	SortableAttribute<DataAudit> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see DataAudit#eventName
	 **/
	TextAttribute<DataAudit> eventName = new TextAttributeRecord<>(EVENT_NAME);
	
	/**
	 * @see DataAudit#columnName
	 **/
	TextAttribute<DataAudit> columnName = new TextAttributeRecord<>(COLUMN_NAME);
	
	/**
	 * @see DataAudit#changedDate
	 **/
	SortableAttribute<DataAudit> changedDate = new SortableAttributeRecord<>(CHANGED_DATE);
	
	/**
	 * @see DataAudit#oldClobValue
	 **/
	TextAttribute<DataAudit> oldClobValue = new TextAttributeRecord<>(OLD_CLOB_VALUE);
	
	/**
	 * @see DataAudit#userId
	 **/
	TextAttribute<DataAudit> userId = new TextAttributeRecord<>(USER_ID);
	
	/**
	 * @see DataAudit#oldValue
	 **/
	TextAttribute<DataAudit> oldValue = new TextAttributeRecord<>(OLD_VALUE);

}

