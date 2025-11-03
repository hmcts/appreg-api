package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;

@StaticMetamodel(NameAddress.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class NameAddress_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity_ {

	public static final String CODE = "code";
	public static final String TELEPHONE_NUMBER = "telephoneNumber";
	public static final String ADDRESS3 = "address3";
	public static final String ADDRESS2 = "address2";
	public static final String ADDRESS1 = "address1";
	public static final String MOBILE_NUMBER = "mobileNumber";
	public static final String FORENAME1 = "forename1";
	public static final String POSTCODE = "postcode";
	public static final String FORENAME2 = "forename2";
	public static final String DATE_OF_BIRTH = "dateOfBirth";
	public static final String TITLE = "title";
	public static final String USER_NAME = "userName";
	public static final String VERSION = "version";
	public static final String FORENAME3 = "forename3";
	public static final String DMS_ID = "dmsId";
	public static final String EMAIL_ADDRESS = "emailAddress";
	public static final String SURNAME = "surname";
	public static final String NAME = "name";
	public static final String ADDRESS5 = "address5";
	public static final String ID = "id";
	public static final String ADDRESS4 = "address4";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#code
	 **/
	public static volatile SingularAttribute<NameAddress, String> code;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#telephoneNumber
	 **/
	public static volatile SingularAttribute<NameAddress, String> telephoneNumber;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#address3
	 **/
	public static volatile SingularAttribute<NameAddress, String> address3;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#address2
	 **/
	public static volatile SingularAttribute<NameAddress, String> address2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#address1
	 **/
	public static volatile SingularAttribute<NameAddress, String> address1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#mobileNumber
	 **/
	public static volatile SingularAttribute<NameAddress, String> mobileNumber;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#forename1
	 **/
	public static volatile SingularAttribute<NameAddress, String> forename1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#postcode
	 **/
	public static volatile SingularAttribute<NameAddress, String> postcode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#forename2
	 **/
	public static volatile SingularAttribute<NameAddress, String> forename2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#dateOfBirth
	 **/
	public static volatile SingularAttribute<NameAddress, OffsetDateTime> dateOfBirth;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#title
	 **/
	public static volatile SingularAttribute<NameAddress, String> title;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#userName
	 **/
	public static volatile SingularAttribute<NameAddress, String> userName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#version
	 **/
	public static volatile SingularAttribute<NameAddress, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#forename3
	 **/
	public static volatile SingularAttribute<NameAddress, String> forename3;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#dmsId
	 **/
	public static volatile SingularAttribute<NameAddress, String> dmsId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#emailAddress
	 **/
	public static volatile SingularAttribute<NameAddress, String> emailAddress;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#surname
	 **/
	public static volatile SingularAttribute<NameAddress, String> surname;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#name
	 **/
	public static volatile SingularAttribute<NameAddress, String> name;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#address5
	 **/
	public static volatile SingularAttribute<NameAddress, String> address5;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#id
	 **/
	public static volatile SingularAttribute<NameAddress, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress#address4
	 **/
	public static volatile SingularAttribute<NameAddress, String> address4;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.NameAddress
	 **/
	public static volatile EntityType<NameAddress> class_;

}

