package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(PettySessionalArea.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _PettySessionalArea extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String COURT_TYPE = "courtType";
	String NAME = "name";
	String CENTRAL_FINANCE_LOC_ID = "centralFinanceLocId";
	String PSA_NAME = "psaName";
	String COURT_LOCATION_CODE = "courtLocationCode";
	String JC_NAME = "jcName";
	String ENFORCED_LOC_ID = "enforcedLocId";
	String ORG_ID = "orgId";
	String CMA_ID = "cmaId";
	String PSA_CODE = "psaCode";
	String END_DATE = "endDate";
	String SHORT_NAME = "shortName";
	String CRIME_CASES_LOC_ID = "crimeCasesLocId";
	String FINE_ACCOUNTS_LOC_ID = "fineAccountsLocId";
	String ID = "id";
	String VERSION = "version";
	String START_DATE = "startDate";
	String FAMILY_CASES_LOC_ID = "familyCasesLocId";

	
	/**
	 * @see PettySessionalArea#courtType
	 **/
	TextAttribute<PettySessionalArea> courtType = new TextAttributeRecord<>(COURT_TYPE);
	
	/**
	 * @see PettySessionalArea#name
	 **/
	TextAttribute<PettySessionalArea> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * @see PettySessionalArea#centralFinanceLocId
	 **/
	SortableAttribute<PettySessionalArea> centralFinanceLocId = new SortableAttributeRecord<>(CENTRAL_FINANCE_LOC_ID);
	
	/**
	 * @see PettySessionalArea#psaName
	 **/
	TextAttribute<PettySessionalArea> psaName = new TextAttributeRecord<>(PSA_NAME);
	
	/**
	 * @see PettySessionalArea#courtLocationCode
	 **/
	TextAttribute<PettySessionalArea> courtLocationCode = new TextAttributeRecord<>(COURT_LOCATION_CODE);
	
	/**
	 * @see PettySessionalArea#jcName
	 **/
	TextAttribute<PettySessionalArea> jcName = new TextAttributeRecord<>(JC_NAME);
	
	/**
	 * @see PettySessionalArea#enforcedLocId
	 **/
	SortableAttribute<PettySessionalArea> enforcedLocId = new SortableAttributeRecord<>(ENFORCED_LOC_ID);
	
	/**
	 * @see PettySessionalArea#orgId
	 **/
	SortableAttribute<PettySessionalArea> orgId = new SortableAttributeRecord<>(ORG_ID);
	
	/**
	 * @see PettySessionalArea#cmaId
	 **/
	SortableAttribute<PettySessionalArea> cmaId = new SortableAttributeRecord<>(CMA_ID);
	
	/**
	 * @see PettySessionalArea#psaCode
	 **/
	TextAttribute<PettySessionalArea> psaCode = new TextAttributeRecord<>(PSA_CODE);
	
	/**
	 * @see PettySessionalArea#endDate
	 **/
	SortableAttribute<PettySessionalArea> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see PettySessionalArea#shortName
	 **/
	TextAttribute<PettySessionalArea> shortName = new TextAttributeRecord<>(SHORT_NAME);
	
	/**
	 * @see PettySessionalArea#crimeCasesLocId
	 **/
	SortableAttribute<PettySessionalArea> crimeCasesLocId = new SortableAttributeRecord<>(CRIME_CASES_LOC_ID);
	
	/**
	 * @see PettySessionalArea#fineAccountsLocId
	 **/
	SortableAttribute<PettySessionalArea> fineAccountsLocId = new SortableAttributeRecord<>(FINE_ACCOUNTS_LOC_ID);
	
	/**
	 * @see PettySessionalArea#id
	 **/
	SortableAttribute<PettySessionalArea> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see PettySessionalArea#version
	 **/
	SortableAttribute<PettySessionalArea> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see PettySessionalArea#startDate
	 **/
	SortableAttribute<PettySessionalArea> startDate = new SortableAttributeRecord<>(START_DATE);
	
	/**
	 * @see PettySessionalArea#familyCasesLocId
	 **/
	SortableAttribute<PettySessionalArea> familyCasesLocId = new SortableAttributeRecord<>(FAMILY_CASES_LOC_ID);

}

