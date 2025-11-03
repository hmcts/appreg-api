package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;

@StaticMetamodel(BaseUnmanagedChangeableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class BaseUnmanagedChangeableEntity_ {

	public static final String CHANGED_DATE = "changedDate";
	public static final String CHANGED_BY = "changedBy";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity#changedDate
	 **/
	public static volatile SingularAttribute<BaseUnmanagedChangeableEntity, OffsetDateTime> changedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity#changedBy
	 **/
	public static volatile SingularAttribute<BaseUnmanagedChangeableEntity, Long> changedBy;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity
	 **/
	public static volatile MappedSuperclassType<BaseUnmanagedChangeableEntity> class_;

}

