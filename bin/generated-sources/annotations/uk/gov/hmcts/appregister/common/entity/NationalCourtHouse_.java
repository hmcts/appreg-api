package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(NationalCourtHouse.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class NationalCourtHouse_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String COURT_TYPE = "courtType";
	public static final String END_DATE = "endDate";
	public static final String LOCATION_ID = "locationId";
	public static final String NAME = "name";
	public static final String PSA_ID = "psaId";
	public static final String COURT_LOCATION_CODE = "courtLocationCode";
	public static final String ID = "id";
	public static final String VERSION = "version";
	public static final String WELSH_NAME = "welshName";
	public static final String START_DATE = "startDate";
	public static final String ORG_ID = "orgId";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#courtType
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, String> courtType;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#endDate
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#locationId
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, Long> locationId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#name
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, String> name;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#psaId
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, PettySessionalArea> psaId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#courtLocationCode
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, String> courtLocationCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#id
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse
	 **/
	public static volatile EntityType<NationalCourtHouse> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#version
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#welshName
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, String> welshName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#startDate
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, LocalDate> startDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NationalCourtHouse#orgId
	 **/
	public static volatile SingularAttribute<NationalCourtHouse, Long> orgId;

}

