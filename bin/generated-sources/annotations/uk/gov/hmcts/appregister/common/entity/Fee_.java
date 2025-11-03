package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@StaticMetamodel(Fee.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Fee_ {

	public static final String REFERENCE = "reference";
	public static final String CHANGED_DATE = "changedDate";
	public static final String AMOUNT = "amount";
	public static final String END_DATE = "endDate";
	public static final String CHANGED_BY = "changedBy";
	public static final String DESCRIPTION = "description";
	public static final String IS_OFFSITE = "isOffsite";
	public static final String ID = "id";
	public static final String VERSION = "version";
	public static final String START_DATE = "startDate";
	public static final String CREATED_USER = "createdUser";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#reference
	 **/
	public static volatile SingularAttribute<Fee, String> reference;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#changedDate
	 **/
	public static volatile SingularAttribute<Fee, OffsetDateTime> changedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#amount
	 **/
	public static volatile SingularAttribute<Fee, BigDecimal> amount;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#endDate
	 **/
	public static volatile SingularAttribute<Fee, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#changedBy
	 **/
	public static volatile SingularAttribute<Fee, Long> changedBy;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#description
	 **/
	public static volatile SingularAttribute<Fee, String> description;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#isOffsite
	 **/
	public static volatile SingularAttribute<Fee, Boolean> isOffsite;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#id
	 **/
	public static volatile SingularAttribute<Fee, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee
	 **/
	public static volatile EntityType<Fee> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#version
	 **/
	public static volatile SingularAttribute<Fee, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#startDate
	 **/
	public static volatile SingularAttribute<Fee, LocalDate> startDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.Fee#createdUser
	 **/
	public static volatile SingularAttribute<Fee, String> createdUser;

}

