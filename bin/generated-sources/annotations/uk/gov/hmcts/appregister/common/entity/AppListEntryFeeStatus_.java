package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;

@StaticMetamodel(AppListEntryFeeStatus.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AppListEntryFeeStatus_ {

	public static final String CHANGED_DATE = "changedDate";
	public static final String ALEFS_PAYMENT_REFERENCE = "alefsPaymentReference";
	public static final String CHANGED_BY = "changedBy";
	public static final String APP_LIST_ENTRY = "appListEntry";
	public static final String ALEFS_FEE_STATUS = "alefsFeeStatus";
	public static final String ALEFS_STATUS_CREATION_DATE = "alefsStatusCreationDate";
	public static final String ID = "id";
	public static final String ALEFS_FEE_STATUS_DATE = "alefsFeeStatusDate";
	public static final String VERSION = "version";
	public static final String CREATED_USER = "createdUser";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#changedDate
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, OffsetDateTime> changedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#alefsPaymentReference
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, String> alefsPaymentReference;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#changedBy
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, String> changedBy;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#appListEntry
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, ApplicationListEntry> appListEntry;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#alefsFeeStatus
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, String> alefsFeeStatus;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#alefsStatusCreationDate
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, OffsetDateTime> alefsStatusCreationDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#id
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus
	 **/
	public static volatile EntityType<AppListEntryFeeStatus> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#alefsFeeStatusDate
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, OffsetDateTime> alefsFeeStatusDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#version
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus#createdUser
	 **/
	public static volatile SingularAttribute<AppListEntryFeeStatus, String> createdUser;

}

