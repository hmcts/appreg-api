package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(LinkCommunicationMedia.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _LinkCommunicationMedia extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String END_DATE = "endDate";
	String BU_ID = "buId";
	String LCM_TYPE = "lcmType";
	String COMM_ID = "commId";
	String ID = "id";
	String VERSION = "version";
	String ER_ID = "erId";
	String START_DATE = "startDate";
	String LOC_ID = "locId";

	
	/**
	 * @see LinkCommunicationMedia#endDate
	 **/
	SortableAttribute<LinkCommunicationMedia> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see LinkCommunicationMedia#buId
	 **/
	SortableAttribute<LinkCommunicationMedia> buId = new SortableAttributeRecord<>(BU_ID);
	
	/**
	 * @see LinkCommunicationMedia#lcmType
	 **/
	TextAttribute<LinkCommunicationMedia> lcmType = new TextAttributeRecord<>(LCM_TYPE);
	
	/**
	 * @see LinkCommunicationMedia#commId
	 **/
	SortableAttribute<LinkCommunicationMedia> commId = new SortableAttributeRecord<>(COMM_ID);
	
	/**
	 * @see LinkCommunicationMedia#id
	 **/
	SortableAttribute<LinkCommunicationMedia> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see LinkCommunicationMedia#version
	 **/
	SortableAttribute<LinkCommunicationMedia> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see LinkCommunicationMedia#erId
	 **/
	SortableAttribute<LinkCommunicationMedia> erId = new SortableAttributeRecord<>(ER_ID);
	
	/**
	 * @see LinkCommunicationMedia#startDate
	 **/
	SortableAttribute<LinkCommunicationMedia> startDate = new SortableAttributeRecord<>(START_DATE);
	
	/**
	 * @see LinkCommunicationMedia#locId
	 **/
	SortableAttribute<LinkCommunicationMedia> locId = new SortableAttributeRecord<>(LOC_ID);

}

