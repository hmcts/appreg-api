package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(Address.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Address extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String LINE2 = "line2";
	String LINE1 = "line1";
	String END_DATE = "endDate";
	String POSTCODE = "postcode";
	String CJA = "cja";
	String ID = "id";
	String LINE5 = "line5";
	String VERSION = "version";
	String LINE4 = "line4";
	String START_DATE = "startDate";
	String LINE3 = "line3";

	
	/**
	 * @see Address#line2
	 **/
	TextAttribute<Address> line2 = new TextAttributeRecord<>(LINE2);
	
	/**
	 * @see Address#line1
	 **/
	TextAttribute<Address> line1 = new TextAttributeRecord<>(LINE1);
	
	/**
	 * @see Address#endDate
	 **/
	SortableAttribute<Address> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see Address#postcode
	 **/
	TextAttribute<Address> postcode = new TextAttributeRecord<>(POSTCODE);
	
	/**
	 * @see Address#cja
	 **/
	SortableAttribute<Address> cja = new SortableAttributeRecord<>(CJA);
	
	/**
	 * @see Address#id
	 **/
	SortableAttribute<Address> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see Address#line5
	 **/
	TextAttribute<Address> line5 = new TextAttributeRecord<>(LINE5);
	
	/**
	 * @see Address#version
	 **/
	SortableAttribute<Address> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see Address#line4
	 **/
	TextAttribute<Address> line4 = new TextAttributeRecord<>(LINE4);
	
	/**
	 * @see Address#startDate
	 **/
	SortableAttribute<Address> startDate = new SortableAttributeRecord<>(START_DATE);
	
	/**
	 * @see Address#line3
	 **/
	TextAttribute<Address> line3 = new TextAttributeRecord<>(LINE3);

}

