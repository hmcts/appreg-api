package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AppListEntryResolution.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AppListEntryResolution_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity_ {

	public static final String RESOLUTION_OFFICER = "resolutionOfficer";
	public static final String RESOLUTION_WORDING = "resolutionWording";
	public static final String RESOLUTION_CODE = "resolutionCode";
	public static final String ID = "id";
	public static final String APPLICATION_LIST = "applicationList";
	public static final String VERSION = "version";
	public static final String CREATED_USER = "createdUser";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#resolutionOfficer
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, String> resolutionOfficer;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#resolutionWording
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, String> resolutionWording;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#resolutionCode
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, ResolutionCode> resolutionCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#id
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#applicationList
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, ApplicationListEntry> applicationList;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution
	 **/
	public static volatile EntityType<AppListEntryResolution> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#version
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.AppListEntryResolution#createdUser
	 **/
	public static volatile SingularAttribute<AppListEntryResolution, String> createdUser;

}

