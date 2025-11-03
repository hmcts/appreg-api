package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(StandardApplicant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class StandardApplicant_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String TELEPHONE_NUMBER = "telephoneNumber";
	public static final String APPLICANT_FORENAME2 = "applicantForename2";
	public static final String APPLICANT_FORENAME1 = "applicantForename1";
	public static final String MOBILE_NUMBER = "mobileNumber";
	public static final String APPLICANT_FORENAME3 = "applicantForename3";
	public static final String POSTCODE = "postcode";
	public static final String VERSION = "version";
	public static final String APPLICANT_CODE = "applicantCode";
	public static final String APPLICANT_END_DATE = "applicantEndDate";
	public static final String EMAIL_ADDRESS = "emailAddress";
	public static final String APPLICANT_SURNAME = "applicantSurname";
	public static final String APPLICANT_START_DATE = "applicantStartDate";
	public static final String NAME = "name";
	public static final String ADDRESS_LINE1 = "addressLine1";
	public static final String ADDRESS_LINE2 = "addressLine2";
	public static final String ID = "id";
	public static final String ADDRESS_LINE3 = "addressLine3";
	public static final String ADDRESS_LINE4 = "addressLine4";
	public static final String ADDRESS_LINE5 = "addressLine5";
	public static final String CREATED_USER = "createdUser";
	public static final String APPLICANT_TITLE = "applicantTitle";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#telephoneNumber
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> telephoneNumber;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantForename2
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> applicantForename2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantForename1
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> applicantForename1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#mobileNumber
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> mobileNumber;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantForename3
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> applicantForename3;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#postcode
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> postcode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#version
	 **/
	public static volatile SingularAttribute<StandardApplicant, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantCode
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> applicantCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantEndDate
	 **/
	public static volatile SingularAttribute<StandardApplicant, LocalDate> applicantEndDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#emailAddress
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> emailAddress;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantSurname
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> applicantSurname;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantStartDate
	 **/
	public static volatile SingularAttribute<StandardApplicant, LocalDate> applicantStartDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#name
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> name;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#addressLine1
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> addressLine1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#addressLine2
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> addressLine2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#id
	 **/
	public static volatile SingularAttribute<StandardApplicant, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#addressLine3
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> addressLine3;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#addressLine4
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> addressLine4;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant
	 **/
	public static volatile EntityType<StandardApplicant> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#addressLine5
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> addressLine5;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#createdUser
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> createdUser;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.StandardApplicant#applicantTitle
	 **/
	public static volatile SingularAttribute<StandardApplicant, String> applicantTitle;

}

