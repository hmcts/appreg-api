package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(Fee.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Fee {

	String REFERENCE = "reference";
	String END_DATE = "endDate";
	String CHANGED_BY = "changedBy";
	String IS_OFFSITE = "isOffsite";
	String ID = "id";
	String DESCRIPTION = "description";
	String CHANGED_DATE = "changedDate";
	String AMOUNT = "amount";
	String VERSION = "version";
	String CREATED_USER = "createdUser";
	String START_DATE = "startDate";

	
	/**
	 * @see Fee#reference
	 **/
	TextAttribute<Fee> reference = new TextAttributeRecord<>(REFERENCE);
	
	/**
	 * @see Fee#endDate
	 **/
	SortableAttribute<Fee> endDate = new SortableAttributeRecord<>(END_DATE);
	
	/**
	 * @see Fee#changedBy
	 **/
	SortableAttribute<Fee> changedBy = new SortableAttributeRecord<>(CHANGED_BY);
	
	/**
	 * @see Fee#isOffsite
	 **/
	SortableAttribute<Fee> isOffsite = new SortableAttributeRecord<>(IS_OFFSITE);
	
	/**
	 * @see Fee#id
	 **/
	SortableAttribute<Fee> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see Fee#description
	 **/
	TextAttribute<Fee> description = new TextAttributeRecord<>(DESCRIPTION);
	
	/**
	 * @see Fee#changedDate
	 **/
	SortableAttribute<Fee> changedDate = new SortableAttributeRecord<>(CHANGED_DATE);
	
	/**
	 * @see Fee#amount
	 **/
	SortableAttribute<Fee> amount = new SortableAttributeRecord<>(AMOUNT);
	
	/**
	 * @see Fee#version
	 **/
	SortableAttribute<Fee> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see Fee#createdUser
	 **/
	TextAttribute<Fee> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see Fee#startDate
	 **/
	SortableAttribute<Fee> startDate = new SortableAttributeRecord<>(START_DATE);

}

