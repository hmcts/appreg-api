package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ApplicationRegister.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ApplicationRegister_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity_ {

	public static final String ID = "id";
	public static final String TEXT = "text";
	public static final String APPLICATION_LIST = "applicationList";
	public static final String USER_NAME = "userName";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationRegister#id
	 **/
	public static volatile SingularAttribute<ApplicationRegister, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationRegister#text
	 **/
	public static volatile SingularAttribute<ApplicationRegister, String> text;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationRegister#applicationList
	 **/
	public static volatile SingularAttribute<ApplicationRegister, ApplicationList> applicationList;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationRegister#userName
	 **/
	public static volatile SingularAttribute<ApplicationRegister, String> userName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationRegister
	 **/
	public static volatile EntityType<ApplicationRegister> class_;

}

