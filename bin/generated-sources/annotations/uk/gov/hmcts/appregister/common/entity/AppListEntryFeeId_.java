package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AppListEntryFeeId.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AppListEntryFeeId_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity_ {

	public static final String APP_LIST_ENTRY_ID = "appListEntryId";
	public static final String LE = "le";
	public static final String FEE_ID = "feeId";
	public static final String VERSION = "version";
	public static final String CREATED_USER = "createdUser";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId#appListEntryId
	 **/
	public static volatile SingularAttribute<AppListEntryFeeId, ApplicationListEntry> appListEntryId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId#le
	 **/
	public static volatile SingularAttribute<AppListEntryFeeId, ApplicationListEntry> le;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId
	 **/
	public static volatile EntityType<AppListEntryFeeId> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId#feeId
	 **/
	public static volatile SingularAttribute<AppListEntryFeeId, Fee> feeId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId#version
	 **/
	public static volatile SingularAttribute<AppListEntryFeeId, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId#createdUser
	 **/
	public static volatile SingularAttribute<AppListEntryFeeId, String> createdUser;

}

