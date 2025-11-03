package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(LinkAddress.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _LinkAddress extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String END_DATE = "endDate";
	String ADDRESSES = "addresses";
	String LA_TYPE = "laType";
	String BU_ID = "buId";
	String NO_FIXED_ABODE = "noFixedAbode";
	String HEAD_OFFICE_INDICATOR = "headOfficeIndicator";
	String ID = "id";
	String VERSION = "version";
	String ER_ID = "erId";
	String START_DATE = "startDate";
	String LOC_ID = "locId";

	
	/**
	 * @see LinkAddress#endDate
	 **/
	SortableAttribute<LinkAddress> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see LinkAddress#addresses
	 **/
	SortableAttribute<LinkAddress> addresses = new SortableAttributeRecord<>(ADDRESSES);
	
	/**
	 * @see LinkAddress#laType
	 **/
	TextAttribute<LinkAddress> laType = new TextAttributeRecord<>(LA_TYPE);
	
	/**
	 * @see LinkAddress#buId
	 **/
	SortableAttribute<LinkAddress> buId = new SortableAttributeRecord<>(BU_ID);
	
	/**
	 * @see LinkAddress#noFixedAbode
	 **/
	TextAttribute<LinkAddress> noFixedAbode = new TextAttributeRecord<>(NO_FIXED_ABODE);
	
	/**
	 * @see LinkAddress#headOfficeIndicator
	 **/
	TextAttribute<LinkAddress> headOfficeIndicator = new TextAttributeRecord<>(HEAD_OFFICE_INDICATOR);
	
	/**
	 * @see LinkAddress#id
	 **/
	SortableAttribute<LinkAddress> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see LinkAddress#version
	 **/
	SortableAttribute<LinkAddress> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see LinkAddress#erId
	 **/
	SortableAttribute<LinkAddress> erId = new SortableAttributeRecord<>(ER_ID);
	
	/**
	 * @see LinkAddress#startDate
	 **/
	SortableAttribute<LinkAddress> startDate = new SortableAttributeRecord<>(START_DATE);
	
	/**
	 * @see LinkAddress#locId
	 **/
	SortableAttribute<LinkAddress> locId = new SortableAttributeRecord<>(LOC_ID);

}

