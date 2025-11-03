package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(PettySessionalArea.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PettySessionalArea_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String COURT_TYPE = "courtType";
	public static final String PSA_CODE = "psaCode";
	public static final String END_DATE = "endDate";
	public static final String FAMILY_CASES_LOC_ID = "familyCasesLocId";
	public static final String CENTRAL_FINANCE_LOC_ID = "centralFinanceLocId";
	public static final String COURT_LOCATION_CODE = "courtLocationCode";
	public static final String VERSION = "version";
	public static final String ORG_ID = "orgId";
	public static final String ENFORCED_LOC_ID = "enforcedLocId";
	public static final String CMA_ID = "cmaId";
	public static final String CRIME_CASES_LOC_ID = "crimeCasesLocId";
	public static final String NAME = "name";
	public static final String PSA_NAME = "psaName";
	public static final String ID = "id";
	public static final String SHORT_NAME = "shortName";
	public static final String JC_NAME = "jcName";
	public static final String FINE_ACCOUNTS_LOC_ID = "fineAccountsLocId";
	public static final String START_DATE = "startDate";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#courtType
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> courtType;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#psaCode
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> psaCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#endDate
	 **/
	public static volatile SingularAttribute<PettySessionalArea, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#familyCasesLocId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> familyCasesLocId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#centralFinanceLocId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> centralFinanceLocId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#courtLocationCode
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> courtLocationCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#version
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#orgId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> orgId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#enforcedLocId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> enforcedLocId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#cmaId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> cmaId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#crimeCasesLocId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> crimeCasesLocId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#name
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> name;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#psaName
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> psaName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#id
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#shortName
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> shortName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea
	 **/
	public static volatile EntityType<PettySessionalArea> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#jcName
	 **/
	public static volatile SingularAttribute<PettySessionalArea, String> jcName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#fineAccountsLocId
	 **/
	public static volatile SingularAttribute<PettySessionalArea, Long> fineAccountsLocId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.PettySessionalArea#startDate
	 **/
	public static volatile SingularAttribute<PettySessionalArea, LocalDate> startDate;

}

