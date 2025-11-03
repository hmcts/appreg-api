package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(NameAddress.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _NameAddress extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableEntity {

	String SURNAME = "surname";
	String ADDRESS5 = "address5";
	String TELEPHONE_NUMBER = "telephoneNumber";
	String NAME = "name";
	String CODE = "code";
	String DATE_OF_BIRTH = "dateOfBirth";
	String POSTCODE = "postcode";
	String MOBILE_NUMBER = "mobileNumber";
	String FORENAME1 = "forename1";
	String TITLE = "title";
	String FORENAME3 = "forename3";
	String FORENAME2 = "forename2";
	String USER_NAME = "userName";
	String DMS_ID = "dmsId";
	String ID = "id";
	String ADDRESS2 = "address2";
	String ADDRESS1 = "address1";
	String EMAIL_ADDRESS = "emailAddress";
	String VERSION = "version";
	String ADDRESS4 = "address4";
	String ADDRESS3 = "address3";

	
	/**
	 * @see NameAddress#surname
	 **/
	TextAttribute<NameAddress> surname = new TextAttributeRecord<>(SURNAME);
	
	/**
	 * @see NameAddress#address5
	 **/
	TextAttribute<NameAddress> address5 = new TextAttributeRecord<>(ADDRESS5);
	
	/**
	 * @see NameAddress#telephoneNumber
	 **/
	TextAttribute<NameAddress> telephoneNumber = new TextAttributeRecord<>(TELEPHONE_NUMBER);
	
	/**
	 * @see NameAddress#name
	 **/
	TextAttribute<NameAddress> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * @see NameAddress#code
	 **/
	TextAttribute<NameAddress> code = new TextAttributeRecord<>(CODE);
	
	/**
	 * @see NameAddress#dateOfBirth
	 **/
	SortableAttribute<NameAddress> dateOfBirth = new SortableAttributeRecord<>(DATE_OF_BIRTH);
	
	/**
	 * @see NameAddress#postcode
	 **/
	TextAttribute<NameAddress> postcode = new TextAttributeRecord<>(POSTCODE);
	
	/**
	 * @see NameAddress#mobileNumber
	 **/
	TextAttribute<NameAddress> mobileNumber = new TextAttributeRecord<>(MOBILE_NUMBER);
	
	/**
	 * @see NameAddress#forename1
	 **/
	TextAttribute<NameAddress> forename1 = new TextAttributeRecord<>(FORENAME1);
	
	/**
	 * @see NameAddress#title
	 **/
	TextAttribute<NameAddress> title = new TextAttributeRecord<>(TITLE);
	
	/**
	 * @see NameAddress#forename3
	 **/
	TextAttribute<NameAddress> forename3 = new TextAttributeRecord<>(FORENAME3);
	
	/**
	 * @see NameAddress#forename2
	 **/
	TextAttribute<NameAddress> forename2 = new TextAttributeRecord<>(FORENAME2);
	
	/**
	 * @see NameAddress#userName
	 **/
	TextAttribute<NameAddress> userName = new TextAttributeRecord<>(USER_NAME);
	
	/**
	 * @see NameAddress#dmsId
	 **/
	TextAttribute<NameAddress> dmsId = new TextAttributeRecord<>(DMS_ID);
	
	/**
	 * @see NameAddress#id
	 **/
	SortableAttribute<NameAddress> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see NameAddress#address2
	 **/
	TextAttribute<NameAddress> address2 = new TextAttributeRecord<>(ADDRESS2);
	
	/**
	 * @see NameAddress#address1
	 **/
	TextAttribute<NameAddress> address1 = new TextAttributeRecord<>(ADDRESS1);
	
	/**
	 * @see NameAddress#emailAddress
	 **/
	TextAttribute<NameAddress> emailAddress = new TextAttributeRecord<>(EMAIL_ADDRESS);
	
	/**
	 * @see NameAddress#version
	 **/
	SortableAttribute<NameAddress> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see NameAddress#address4
	 **/
	TextAttribute<NameAddress> address4 = new TextAttributeRecord<>(ADDRESS4);
	
	/**
	 * @see NameAddress#address3
	 **/
	TextAttribute<NameAddress> address3 = new TextAttributeRecord<>(ADDRESS3);

}

