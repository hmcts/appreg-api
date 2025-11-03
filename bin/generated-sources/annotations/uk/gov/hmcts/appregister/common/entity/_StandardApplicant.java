package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(StandardApplicant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _StandardApplicant extends uk.gov.hmcts.appregister.common.entity.base._BaseUnmanagedChangeableEntity {

	String APPLICANT_TITLE = "applicantTitle";
	String APPLICANT_FORENAME1 = "applicantForename1";
	String TELEPHONE_NUMBER = "telephoneNumber";
	String ADDRESS_LINE5 = "addressLine5";
	String NAME = "name";
	String ADDRESS_LINE4 = "addressLine4";
	String ADDRESS_LINE3 = "addressLine3";
	String ADDRESS_LINE2 = "addressLine2";
	String ADDRESS_LINE1 = "addressLine1";
	String POSTCODE = "postcode";
	String MOBILE_NUMBER = "mobileNumber";
	String APPLICANT_SURNAME = "applicantSurname";
	String CREATED_USER = "createdUser";
	String APPLICANT_FORENAME3 = "applicantForename3";
	String APPLICANT_FORENAME2 = "applicantForename2";
	String APPLICANT_END_DATE = "applicantEndDate";
	String ID = "id";
	String VERSION = "version";
	String EMAIL_ADDRESS = "emailAddress";
	String APPLICANT_START_DATE = "applicantStartDate";
	String APPLICANT_CODE = "applicantCode";

	
	/**
	 * @see StandardApplicant#applicantTitle
	 **/
	TextAttribute<StandardApplicant> applicantTitle = new TextAttributeRecord<>(APPLICANT_TITLE);
	
	/**
	 * @see StandardApplicant#applicantForename1
	 **/
	TextAttribute<StandardApplicant> applicantForename1 = new TextAttributeRecord<>(APPLICANT_FORENAME1);
	
	/**
	 * @see StandardApplicant#telephoneNumber
	 **/
	TextAttribute<StandardApplicant> telephoneNumber = new TextAttributeRecord<>(TELEPHONE_NUMBER);
	
	/**
	 * @see StandardApplicant#addressLine5
	 **/
	TextAttribute<StandardApplicant> addressLine5 = new TextAttributeRecord<>(ADDRESS_LINE5);
	
	/**
	 * @see StandardApplicant#name
	 **/
	TextAttribute<StandardApplicant> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * @see StandardApplicant#addressLine4
	 **/
	TextAttribute<StandardApplicant> addressLine4 = new TextAttributeRecord<>(ADDRESS_LINE4);
	
	/**
	 * @see StandardApplicant#addressLine3
	 **/
	TextAttribute<StandardApplicant> addressLine3 = new TextAttributeRecord<>(ADDRESS_LINE3);
	
	/**
	 * @see StandardApplicant#addressLine2
	 **/
	TextAttribute<StandardApplicant> addressLine2 = new TextAttributeRecord<>(ADDRESS_LINE2);
	
	/**
	 * @see StandardApplicant#addressLine1
	 **/
	TextAttribute<StandardApplicant> addressLine1 = new TextAttributeRecord<>(ADDRESS_LINE1);
	
	/**
	 * @see StandardApplicant#postcode
	 **/
	TextAttribute<StandardApplicant> postcode = new TextAttributeRecord<>(POSTCODE);
	
	/**
	 * @see StandardApplicant#mobileNumber
	 **/
	TextAttribute<StandardApplicant> mobileNumber = new TextAttributeRecord<>(MOBILE_NUMBER);
	
	/**
	 * @see StandardApplicant#applicantSurname
	 **/
	TextAttribute<StandardApplicant> applicantSurname = new TextAttributeRecord<>(APPLICANT_SURNAME);
	
	/**
	 * @see StandardApplicant#createdUser
	 **/
	TextAttribute<StandardApplicant> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see StandardApplicant#applicantForename3
	 **/
	TextAttribute<StandardApplicant> applicantForename3 = new TextAttributeRecord<>(APPLICANT_FORENAME3);
	
	/**
	 * @see StandardApplicant#applicantForename2
	 **/
	TextAttribute<StandardApplicant> applicantForename2 = new TextAttributeRecord<>(APPLICANT_FORENAME2);
	
	/**
	 * @see StandardApplicant#applicantEndDate
	 **/
	SortableAttribute<StandardApplicant> applicantEndDate = new SortableAttributeRecord<>(APPLICANT_END_DATE);
	
	/**
	 * @see StandardApplicant#id
	 **/
	SortableAttribute<StandardApplicant> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see StandardApplicant#version
	 **/
	SortableAttribute<StandardApplicant> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see StandardApplicant#emailAddress
	 **/
	TextAttribute<StandardApplicant> emailAddress = new TextAttributeRecord<>(EMAIL_ADDRESS);
	
	/**
	 * @see StandardApplicant#applicantStartDate
	 **/
	SortableAttribute<StandardApplicant> applicantStartDate = new SortableAttributeRecord<>(APPLICANT_START_DATE);
	
	/**
	 * @see StandardApplicant#applicantCode
	 **/
	TextAttribute<StandardApplicant> applicantCode = new TextAttributeRecord<>(APPLICANT_CODE);

}

