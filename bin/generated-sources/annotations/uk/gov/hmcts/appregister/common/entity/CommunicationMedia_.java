package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(CommunicationMedia.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CommunicationMedia_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String END_DATE = "endDate";
	public static final String ID = "id";
	public static final String DETAIL = "detail";
	public static final String VERSION = "version";
	public static final String START_DATE = "startDate";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CommunicationMedia#endDate
	 **/
	public static volatile SingularAttribute<CommunicationMedia, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CommunicationMedia#id
	 **/
	public static volatile SingularAttribute<CommunicationMedia, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CommunicationMedia#detail
	 **/
	public static volatile SingularAttribute<CommunicationMedia, String> detail;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CommunicationMedia
	 **/
	public static volatile EntityType<CommunicationMedia> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CommunicationMedia#version
	 **/
	public static volatile SingularAttribute<CommunicationMedia, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CommunicationMedia#startDate
	 **/
	public static volatile SingularAttribute<CommunicationMedia, LocalDate> startDate;

}

