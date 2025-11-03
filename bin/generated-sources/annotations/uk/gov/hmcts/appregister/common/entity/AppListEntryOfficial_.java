package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AppListEntryOfficial.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AppListEntryOfficial_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity_ {

	public static final String FORENAME = "forename";
	public static final String SURNAME = "surname";
	public static final String OFFICIAL_TYPE = "officialType";
	public static final String APP_LIST_ENTRY = "appListEntry";
	public static final String ID = "id";
	public static final String TITLE = "title";
	public static final String CREATED_USER = "createdUser";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#forename
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, String> forename;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#surname
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, String> surname;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#officialType
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, String> officialType;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#appListEntry
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, ApplicationListEntry> appListEntry;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#id
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#title
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, String> title;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial
	 **/
	public static volatile EntityType<AppListEntryOfficial> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial#createdUser
	 **/
	public static volatile SingularAttribute<AppListEntryOfficial, String> createdUser;

}

