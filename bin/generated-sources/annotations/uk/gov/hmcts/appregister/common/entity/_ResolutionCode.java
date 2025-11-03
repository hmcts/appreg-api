package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(ResolutionCode.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _ResolutionCode extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String END_DATE = "endDate";
	String LEGISLATION = "legislation";
	String RESULT_CODE = "resultCode";
	String TITLE = "title";
	String ID = "id";
	String DESTINATION_EMAIL2 = "destinationEmail2";
	String WORDING = "wording";
	String DESTINATION_EMAIL1 = "destinationEmail1";
	String VERSION = "version";
	String CREATED_USER = "createdUser";
	String START_DATE = "startDate";

	
	/**
	 * @see ResolutionCode#endDate
	 **/
	SortableAttribute<ResolutionCode> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see ResolutionCode#legislation
	 **/
	TextAttribute<ResolutionCode> legislation = new TextAttributeRecord<>(LEGISLATION);
	
	/**
	 * @see ResolutionCode#resultCode
	 **/
	TextAttribute<ResolutionCode> resultCode = new TextAttributeRecord<>(RESULT_CODE);
	
	/**
	 * @see ResolutionCode#title
	 **/
	TextAttribute<ResolutionCode> title = new TextAttributeRecord<>(TITLE);
	
	/**
	 * @see ResolutionCode#id
	 **/
	SortableAttribute<ResolutionCode> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see ResolutionCode#destinationEmail2
	 **/
	TextAttribute<ResolutionCode> destinationEmail2 = new TextAttributeRecord<>(DESTINATION_EMAIL2);
	
	/**
	 * @see ResolutionCode#wording
	 **/
	TextAttribute<ResolutionCode> wording = new TextAttributeRecord<>(WORDING);
	
	/**
	 * @see ResolutionCode#destinationEmail1
	 **/
	TextAttribute<ResolutionCode> destinationEmail1 = new TextAttributeRecord<>(DESTINATION_EMAIL1);
	
	/**
	 * @see ResolutionCode#version
	 **/
	SortableAttribute<ResolutionCode> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see ResolutionCode#createdUser
	 **/
	TextAttribute<ResolutionCode> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see ResolutionCode#startDate
	 **/
	SortableAttribute<ResolutionCode> startDate = new SortableAttributeRecord<>(START_DATE);

}

