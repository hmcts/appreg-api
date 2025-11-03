package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(ApplicationList.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _ApplicationList extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableAndDeletableEntity {

	String OTHER_LOCATION = "otherLocation";
	String DATE = "date";
	String COURT_CODE = "courtCode";
	String UUID = "uuid";
	String STATUS = "status";
	String CREATED_USER = "createdUser";
	String DURATION_MINUTES = "durationMinutes";
	String COURT_NAME = "courtName";
	String DURATION_HOURS = "durationHours";
	String CJA = "cja";
	String PK = "pk";
	String DESCRIPTION = "description";
	String TIME = "time";
	String VERSION = "version";

	
	/**
	 * @see ApplicationList#otherLocation
	 **/
	TextAttribute<ApplicationList> otherLocation = new TextAttributeRecord<>(OTHER_LOCATION);
	
	/**
	 * @see ApplicationList#date
	 **/
	SortableAttribute<ApplicationList> date = new SortableAttributeRecord<>(DATE);
	
	/**
	 * @see ApplicationList#courtCode
	 **/
	TextAttribute<ApplicationList> courtCode = new TextAttributeRecord<>(COURT_CODE);
	
	/**
	 * @see ApplicationList#uuid
	 **/
	SortableAttribute<ApplicationList> uuid = new SortableAttributeRecord<>(UUID);
	
	/**
	 * @see ApplicationList#status
	 **/
	SortableAttribute<ApplicationList> status = new SortableAttributeRecord<>(STATUS);
	
	/**
	 * @see ApplicationList#createdUser
	 **/
	TextAttribute<ApplicationList> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see ApplicationList#durationMinutes
	 **/
	SortableAttribute<ApplicationList> durationMinutes = new SortableAttributeRecord<>(DURATION_MINUTES);
	
	/**
	 * @see ApplicationList#courtName
	 **/
	TextAttribute<ApplicationList> courtName = new TextAttributeRecord<>(COURT_NAME);
	
	/**
	 * @see ApplicationList#durationHours
	 **/
	SortableAttribute<ApplicationList> durationHours = new SortableAttributeRecord<>(DURATION_HOURS);
	
	/**
	 * @see ApplicationList#cja
	 **/
	SortableAttribute<ApplicationList> cja = new SortableAttributeRecord<>(CJA);
	
	/**
	 * @see ApplicationList#pk
	 **/
	SortableAttribute<ApplicationList> pk = new SortableAttributeRecord<>(PK);
	
	/**
	 * @see ApplicationList#description
	 **/
	TextAttribute<ApplicationList> description = new TextAttributeRecord<>(DESCRIPTION);
	
	/**
	 * @see ApplicationList#time
	 **/
	SortableAttribute<ApplicationList> time = new SortableAttributeRecord<>(TIME);
	
	/**
	 * @see ApplicationList#version
	 **/
	SortableAttribute<ApplicationList> version = new SortableAttributeRecord<>(VERSION);

}

