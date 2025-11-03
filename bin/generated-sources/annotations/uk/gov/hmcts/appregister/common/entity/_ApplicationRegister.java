package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(ApplicationRegister.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _ApplicationRegister extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableEntity {

	String USER_NAME = "userName";
	String APPLICATION_LIST = "applicationList";
	String ID = "id";
	String TEXT = "text";

	
	/**
	 * @see ApplicationRegister#userName
	 **/
	TextAttribute<ApplicationRegister> userName = new TextAttributeRecord<>(USER_NAME);
	
	/**
	 * @see ApplicationRegister#applicationList
	 **/
	SortableAttribute<ApplicationRegister> applicationList = new SortableAttributeRecord<>(APPLICATION_LIST);
	
	/**
	 * @see ApplicationRegister#id
	 **/
	SortableAttribute<ApplicationRegister> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see ApplicationRegister#text
	 **/
	TextAttribute<ApplicationRegister> text = new TextAttributeRecord<>(TEXT);

}

