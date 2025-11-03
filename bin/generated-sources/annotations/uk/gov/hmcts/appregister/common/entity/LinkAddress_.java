package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(LinkAddress.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class LinkAddress_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String BU_ID = "buId";
	public static final String NO_FIXED_ABODE = "noFixedAbode";
	public static final String ADDRESSES = "addresses";
	public static final String LA_TYPE = "laType";
	public static final String END_DATE = "endDate";
	public static final String ER_ID = "erId";
	public static final String ID = "id";
	public static final String HEAD_OFFICE_INDICATOR = "headOfficeIndicator";
	public static final String VERSION = "version";
	public static final String START_DATE = "startDate";
	public static final String LOC_ID = "locId";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#buId
	 **/
	public static volatile SingularAttribute<LinkAddress, Long> buId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#noFixedAbode
	 **/
	public static volatile SingularAttribute<LinkAddress, String> noFixedAbode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#addresses
	 **/
	public static volatile SingularAttribute<LinkAddress, Address> addresses;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#laType
	 **/
	public static volatile SingularAttribute<LinkAddress, String> laType;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#endDate
	 **/
	public static volatile SingularAttribute<LinkAddress, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#erId
	 **/
	public static volatile SingularAttribute<LinkAddress, Long> erId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#id
	 **/
	public static volatile SingularAttribute<LinkAddress, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#headOfficeIndicator
	 **/
	public static volatile SingularAttribute<LinkAddress, String> headOfficeIndicator;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress
	 **/
	public static volatile EntityType<LinkAddress> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#version
	 **/
	public static volatile SingularAttribute<LinkAddress, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#startDate
	 **/
	public static volatile SingularAttribute<LinkAddress, LocalDate> startDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkAddress#locId
	 **/
	public static volatile SingularAttribute<LinkAddress, Long> locId;

}

