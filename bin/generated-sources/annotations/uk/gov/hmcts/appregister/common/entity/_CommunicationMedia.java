package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(CommunicationMedia.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _CommunicationMedia extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String END_DATE = "endDate";
	String DETAIL = "detail";
	String ID = "id";
	String VERSION = "version";
	String START_DATE = "startDate";

	
	/**
	 * @see CommunicationMedia#endDate
	 **/
	SortableAttribute<CommunicationMedia> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see CommunicationMedia#detail
	 **/
	TextAttribute<CommunicationMedia> detail = new TextAttributeRecord<>(DETAIL);
	
	/**
	 * @see CommunicationMedia#id
	 **/
	SortableAttribute<CommunicationMedia> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see CommunicationMedia#version
	 **/
	SortableAttribute<CommunicationMedia> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see CommunicationMedia#startDate
	 **/
	SortableAttribute<CommunicationMedia> startDate = new SortableAttributeRecord<>(START_DATE);

}

