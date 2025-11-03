package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(AppListEntryFeeStatus.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _AppListEntryFeeStatus {

	String ALEFS_FEE_STATUS_DATE = "alefsFeeStatusDate";
	String CHANGED_BY = "changedBy";
	String ALEFS_PAYMENT_REFERENCE = "alefsPaymentReference";
	String ALEFS_STATUS_CREATION_DATE = "alefsStatusCreationDate";
	String ALEFS_FEE_STATUS = "alefsFeeStatus";
	String ID = "id";
	String APP_LIST_ENTRY = "appListEntry";
	String CHANGED_DATE = "changedDate";
	String VERSION = "version";
	String CREATED_USER = "createdUser";

	
	/**
	 * @see AppListEntryFeeStatus#alefsFeeStatusDate
	 **/
	SortableAttribute<AppListEntryFeeStatus> alefsFeeStatusDate = new SortableAttributeRecord<>(ALEFS_FEE_STATUS_DATE);
	
	/**
	 * @see AppListEntryFeeStatus#changedBy
	 **/
	TextAttribute<AppListEntryFeeStatus> changedBy = new TextAttributeRecord<>(CHANGED_BY);
	
	/**
	 * @see AppListEntryFeeStatus#alefsPaymentReference
	 **/
	TextAttribute<AppListEntryFeeStatus> alefsPaymentReference = new TextAttributeRecord<>(ALEFS_PAYMENT_REFERENCE);
	
	/**
	 * @see AppListEntryFeeStatus#alefsStatusCreationDate
	 **/
	SortableAttribute<AppListEntryFeeStatus> alefsStatusCreationDate = new SortableAttributeRecord<>(ALEFS_STATUS_CREATION_DATE);
	
	/**
	 * @see AppListEntryFeeStatus#alefsFeeStatus
	 **/
	TextAttribute<AppListEntryFeeStatus> alefsFeeStatus = new TextAttributeRecord<>(ALEFS_FEE_STATUS);
	
	/**
	 * @see AppListEntryFeeStatus#id
	 **/
	SortableAttribute<AppListEntryFeeStatus> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see AppListEntryFeeStatus#appListEntry
	 **/
	SortableAttribute<AppListEntryFeeStatus> appListEntry = new SortableAttributeRecord<>(APP_LIST_ENTRY);
	
	/**
	 * @see AppListEntryFeeStatus#changedDate
	 **/
	SortableAttribute<AppListEntryFeeStatus> changedDate = new SortableAttributeRecord<>(CHANGED_DATE);
	
	/**
	 * @see AppListEntryFeeStatus#version
	 **/
	SortableAttribute<AppListEntryFeeStatus> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see AppListEntryFeeStatus#createdUser
	 **/
	TextAttribute<AppListEntryFeeStatus> createdUser = new TextAttributeRecord<>(CREATED_USER);

}

