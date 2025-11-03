package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(AppListEntryFeeId.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _AppListEntryFeeId extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableEntity {

	String LE = "le";
	String APP_LIST_ENTRY_ID = "appListEntryId";
	String FEE_ID = "feeId";
	String VERSION = "version";
	String CREATED_USER = "createdUser";

	
	/**
	 * @see AppListEntryFeeId#le
	 **/
	SortableAttribute<AppListEntryFeeId> le = new SortableAttributeRecord<>(LE);
	
	/**
	 * @see AppListEntryFeeId#appListEntryId
	 **/
	SortableAttribute<AppListEntryFeeId> appListEntryId = new SortableAttributeRecord<>(APP_LIST_ENTRY_ID);
	
	/**
	 * @see AppListEntryFeeId#feeId
	 **/
	SortableAttribute<AppListEntryFeeId> feeId = new SortableAttributeRecord<>(FEE_ID);
	
	/**
	 * @see AppListEntryFeeId#version
	 **/
	SortableAttribute<AppListEntryFeeId> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see AppListEntryFeeId#createdUser
	 **/
	TextAttribute<AppListEntryFeeId> createdUser = new TextAttributeRecord<>(CREATED_USER);

}

