package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(NationalCourtHouse.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _NationalCourtHouse extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String COURT_TYPE = "courtType";
	String END_DATE = "endDate";
	String NAME = "name";
	String PSA_ID = "psaId";
	String COURT_LOCATION_CODE = "courtLocationCode";
	String ID = "id";
	String LOCATION_ID = "locationId";
	String WELSH_NAME = "welshName";
	String VERSION = "version";
	String ORG_ID = "orgId";
	String START_DATE = "startDate";

	
	/**
	 * @see NationalCourtHouse#courtType
	 **/
	TextAttribute<NationalCourtHouse> courtType = new TextAttributeRecord<>(COURT_TYPE);
	
	/**
	 * @see NationalCourtHouse#endDate
	 **/
	SortableAttribute<NationalCourtHouse> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see NationalCourtHouse#name
	 **/
	TextAttribute<NationalCourtHouse> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * @see NationalCourtHouse#psaId
	 **/
	SortableAttribute<NationalCourtHouse> psaId = new SortableAttributeRecord<>(PSA_ID);
	
	/**
	 * @see NationalCourtHouse#courtLocationCode
	 **/
	TextAttribute<NationalCourtHouse> courtLocationCode = new TextAttributeRecord<>(COURT_LOCATION_CODE);
	
	/**
	 * @see NationalCourtHouse#id
	 **/
	SortableAttribute<NationalCourtHouse> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see NationalCourtHouse#locationId
	 **/
	SortableAttribute<NationalCourtHouse> locationId = new SortableAttributeRecord<>(LOCATION_ID);
	
	/**
	 * @see NationalCourtHouse#welshName
	 **/
	TextAttribute<NationalCourtHouse> welshName = new TextAttributeRecord<>(WELSH_NAME);
	
	/**
	 * @see NationalCourtHouse#version
	 **/
	SortableAttribute<NationalCourtHouse> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see NationalCourtHouse#orgId
	 **/
	SortableAttribute<NationalCourtHouse> orgId = new SortableAttributeRecord<>(ORG_ID);
	
	/**
	 * @see NationalCourtHouse#startDate
	 **/
	SortableAttribute<NationalCourtHouse> startDate = new SortableAttributeRecord<>(START_DATE);

}

