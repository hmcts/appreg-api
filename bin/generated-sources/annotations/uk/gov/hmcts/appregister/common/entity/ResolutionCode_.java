package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(ResolutionCode.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ResolutionCode_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String DESTINATION_EMAIL2 = "destinationEmail2";
	public static final String DESTINATION_EMAIL1 = "destinationEmail1";
	public static final String END_DATE = "endDate";
	public static final String RESULT_CODE = "resultCode";
	public static final String ID = "id";
	public static final String TITLE = "title";
	public static final String WORDING = "wording";
	public static final String VERSION = "version";
	public static final String START_DATE = "startDate";
	public static final String CREATED_USER = "createdUser";
	public static final String LEGISLATION = "legislation";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#destinationEmail2
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> destinationEmail2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#destinationEmail1
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> destinationEmail1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#endDate
	 **/
	public static volatile SingularAttribute<ResolutionCode, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#resultCode
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> resultCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#id
	 **/
	public static volatile SingularAttribute<ResolutionCode, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#title
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> title;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#wording
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> wording;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode
	 **/
	public static volatile EntityType<ResolutionCode> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#version
	 **/
	public static volatile SingularAttribute<ResolutionCode, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#startDate
	 **/
	public static volatile SingularAttribute<ResolutionCode, LocalDate> startDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#createdUser
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> createdUser;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ResolutionCode#legislation
	 **/
	public static volatile SingularAttribute<ResolutionCode, String> legislation;

}

