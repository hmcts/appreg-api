package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(ApplicationCode.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _ApplicationCode extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String CODE = "code";
	String LEGISLATION = "legislation";
	String TITLE = "title";
	String DESTINATION_EMAIL2 = "destinationEmail2";
	String FEE_REFERENCE = "feeReference";
	String WORDING = "wording";
	String DESTINATION_EMAIL1 = "destinationEmail1";
	String FEE_DUE = "feeDue";
	String END_DATE = "endDate";
	String REQUIRES_RESPONDENT = "requiresRespondent";
	String USER_NAME = "userName";
	String BULK_RESPONDENT_ALLOWED = "bulkRespondentAllowed";
	String ID = "id";
	String VERSION = "version";
	String START_DATE = "startDate";

	
	/**
	 * @see ApplicationCode#code
	 **/
	TextAttribute<ApplicationCode> code = new TextAttributeRecord<>(CODE);
	
	/**
	 * @see ApplicationCode#legislation
	 **/
	TextAttribute<ApplicationCode> legislation = new TextAttributeRecord<>(LEGISLATION);
	
	/**
	 * @see ApplicationCode#title
	 **/
	TextAttribute<ApplicationCode> title = new TextAttributeRecord<>(TITLE);
	
	/**
	 * @see ApplicationCode#destinationEmail2
	 **/
	TextAttribute<ApplicationCode> destinationEmail2 = new TextAttributeRecord<>(DESTINATION_EMAIL2);
	
	/**
	 * @see ApplicationCode#feeReference
	 **/
	TextAttribute<ApplicationCode> feeReference = new TextAttributeRecord<>(FEE_REFERENCE);
	
	/**
	 * @see ApplicationCode#wording
	 **/
	TextAttribute<ApplicationCode> wording = new TextAttributeRecord<>(WORDING);
	
	/**
	 * @see ApplicationCode#destinationEmail1
	 **/
	TextAttribute<ApplicationCode> destinationEmail1 = new TextAttributeRecord<>(DESTINATION_EMAIL1);
	
	/**
	 * @see ApplicationCode#feeDue
	 **/
	SortableAttribute<ApplicationCode> feeDue = new SortableAttributeRecord<>(FEE_DUE);
	
	/**
	 * @see ApplicationCode#endDate
	 **/
	SortableAttribute<ApplicationCode> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see ApplicationCode#requiresRespondent
	 **/
	SortableAttribute<ApplicationCode> requiresRespondent = new SortableAttributeRecord<>(REQUIRES_RESPONDENT);
	
	/**
	 * @see ApplicationCode#userName
	 **/
	TextAttribute<ApplicationCode> userName = new TextAttributeRecord<>(USER_NAME);
	
	/**
	 * @see ApplicationCode#bulkRespondentAllowed
	 **/
	SortableAttribute<ApplicationCode> bulkRespondentAllowed = new SortableAttributeRecord<>(BULK_RESPONDENT_ALLOWED);
	
	/**
	 * @see ApplicationCode#id
	 **/
	SortableAttribute<ApplicationCode> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see ApplicationCode#version
	 **/
	SortableAttribute<ApplicationCode> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see ApplicationCode#startDate
	 **/
	SortableAttribute<ApplicationCode> startDate = new SortableAttributeRecord<>(START_DATE);

}

