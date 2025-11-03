package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(Address.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Address_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String LINE5 = "line5";
	public static final String LINE4 = "line4";
	public static final String END_DATE = "endDate";
	public static final String POSTCODE = "postcode";
	public static final String ID = "id";
	public static final String CJA = "cja";
	public static final String LINE3 = "line3";
	public static final String LINE2 = "line2";
	public static final String VERSION = "version";
	public static final String LINE1 = "line1";
	public static final String START_DATE = "startDate";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#line5
	 **/
	public static volatile SingularAttribute<Address, String> line5;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#line4
	 **/
	public static volatile SingularAttribute<Address, String> line4;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#endDate
	 **/
	public static volatile SingularAttribute<Address, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#postcode
	 **/
	public static volatile SingularAttribute<Address, String> postcode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#id
	 **/
	public static volatile SingularAttribute<Address, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#cja
	 **/
	public static volatile SingularAttribute<Address, Long> cja;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#line3
	 **/
	public static volatile SingularAttribute<Address, String> line3;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address
	 **/
	public static volatile EntityType<Address> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#line2
	 **/
	public static volatile SingularAttribute<Address, String> line2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#version
	 **/
	public static volatile SingularAttribute<Address, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#line1
	 **/
	public static volatile SingularAttribute<Address, String> line1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Address#startDate
	 **/
	public static volatile SingularAttribute<Address, LocalDate> startDate;

}

